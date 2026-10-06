package com.ruzinn.serviceimpl;

import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.service.ItemMasterExcelService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ItemMasterExcelServiceImpl implements ItemMasterExcelService {

  private static final Logger log = LoggerFactory.getLogger(ItemMasterExcelServiceImpl.class);

  static final List<String> HEADERS =
      List.of(
          "Experience / Ticket",
          "Country",
          "Dated Type",
          "Category",
          "API TimeSlot",
          "API Booking",
          "Adult",
          "Child",
          "Booking_info",
          "Fields1",
          "Fields2",
          "Fields3",
          "Fields4",
          "Fields5",
          "Fields6",
          "Fields7",
          "Fields8",
          "Fields9",
          "Fields10",
          "Fields11",
          "Fields12",
          "Fields13",
          "Fields14",
          "Fields15",
          "Description",
          "Terms & Condition",
          "Included",
          "Excluded",
          "Image in PDF",
          "Image in ticket");

  private static final DataFormatter FORMATTER = new DataFormatter();

  @Override
  public byte[] template() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet notes = workbook.createSheet("Notes");
      notes.createRow(0).createCell(0).setCellValue("Park / items master template");
      notes.createRow(1)
          .createCell(0)
          .setCellValue("Fill the Parks sheet and upload it on Items to create or update bulk items.");
      notes.createRow(2).createCell(0).setCellValue("Dated Type: DA = Dated, DT = With timeslot");
      notes.createRow(3).createCell(0).setCellValue("API TimeSlot / API Booking: Y or N");
      notes.createRow(4)
          .createCell(0)
          .setCellValue("Booking_info: DFT = default passenger fields, ADD_ON = extra Fields1-15 required");
      notes.createRow(5).createCell(0).setCellValue("Adult / Child are AED rates. Leave Child blank when not used.");
      notes.createRow(6).createCell(0).setCellValue("Country: UAE, IND (India), or SIN (Singapore). New booking only lists items for the selected country.");
      notes.setColumnWidth(0, 12000);

      Sheet parks = workbook.createSheet("Parks");
      Row header = parks.createRow(0);
      for (int i = 0; i < HEADERS.size(); i++) {
        header.createCell(i).setCellValue(HEADERS.get(i));
        parks.setColumnWidth(i, i == 0 || i >= 23 ? 6000 : 3200);
      }
      Row sample = parks.createRow(1);
      sample.createCell(0).setCellValue("AYA Universe (Dubai)");
      sample.createCell(1).setCellValue("UAE");
      sample.createCell(2).setCellValue("DA");
      sample.createCell(3).setCellValue("Adventure");
      sample.createCell(4).setCellValue("N");
      sample.createCell(5).setCellValue("N");
      sample.createCell(6).setCellValue(95);
      sample.createCell(8).setCellValue("DFT");
      sample.createCell(24)
          .setCellValue("AYA is an immersive entertainment park where art and technology combine.");
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Could not build the items template", ex);
    }
  }

  @Override
  public List<ParsedRow> parse(InputStream input) {
    try (Workbook workbook = new XSSFWorkbook(input)) {
      Sheet sheet = workbook.getSheet("Parks");
      if (sheet == null) {
        sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
      }
      if (sheet == null) {
        throw new IllegalArgumentException("The Excel file has no Parks sheet");
      }
      HeaderMap headers = findHeader(sheet);
      List<ParsedRow> rows = new ArrayList<>();
      for (int r = headers.rowIndex() + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        String name = cell(row, headers.index("experience / ticket"));
        if (name.isBlank()) {
          continue;
        }
        ItemMasterForm form = new ItemMasterForm();
        form.setItemName(name);
        form.setCountry(cell(row, headers.index("country")));
        form.setDatedType(cell(row, headers.index("dated type")));
        form.setCategory(cell(row, headers.index("category")));
        form.setApiTimeslot(yes(cell(row, headers.index("api timeslot"))));
        form.setApiBooking(yes(cell(row, headers.index("api booking"))));
        form.setAdultRate(cell(row, headers.index("adult")));
        form.setChildRate(cell(row, headers.index("child")));
        form.setBookingInfo(cell(row, headers.index("booking_info")));
        List<String> extra = new ArrayList<>(15);
        for (int i = 1; i <= 15; i++) {
          extra.add(cell(row, headers.index("fields" + i)));
        }
        form.setExtraFields(extra);
        form.setDescription(cell(row, headers.index("description")));
        form.setTermsAndCondition(cell(row, headers.index("terms & condition")));
        form.setIncluded(cell(row, headers.index("included")));
        form.setExcluded(cell(row, headers.index("excluded")));
        form.setImagePdf(cell(row, headers.index("image in pdf")));
        form.setImageTicket(cell(row, headers.index("image in ticket")));
        rows.add(new ParsedRow(r + 1, form));
      }
      if (rows.isEmpty()) {
        log.warn("Excel sheet '{}' has no item rows", sheet.getSheetName());
        throw new IllegalArgumentException("No item rows found. Use the Parks sheet headers from the template.");
      }
      log.info("Parsed {} item row(s) from Excel sheet '{}'", rows.size(), sheet.getSheetName());
      return rows;
    } catch (IOException ex) {
      log.warn("Could not read uploaded Excel file: {}", ex.getMessage());
      throw new IllegalArgumentException("Could not read the Excel file. Upload an .xlsx template.");
    }
  }

  private static HeaderMap findHeader(Sheet sheet) {
    for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      Map<String, Integer> index = new LinkedHashMap<>();
      for (Cell cell : row) {
        String key = normalizeHeader(FORMATTER.formatCellValue(cell));
        if (!key.isBlank()) {
          index.putIfAbsent(key, cell.getColumnIndex());
        }
      }
      if (index.containsKey("experience / ticket") && index.containsKey("dated type")) {
        return new HeaderMap(r, index);
      }
    }
    throw new IllegalArgumentException(
        "Could not find the header row. Download the template and keep the Parks column titles.");
  }

  static String normalizeHeader(String value) {
    if (value == null) {
      return "";
    }
    return value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }

  private static String cell(Row row, Integer index) {
    if (index == null) {
      return "";
    }
    Cell cell = row.getCell(index);
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      double number = cell.getNumericCellValue();
      if (number == Math.rint(number) && Math.abs(number) < 1_000_000_000d) {
        return BigDecimal.valueOf((long) number).toPlainString();
      }
      return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
    return FORMATTER.formatCellValue(cell).trim();
  }

  private static boolean yes(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    return "Y".equals(normalized) || "YES".equals(normalized) || "TRUE".equals(normalized);
  }

  private record HeaderMap(int rowIndex, Map<String, Integer> index) {
    Integer index(String header) {
      return index.get(header);
    }
  }
}

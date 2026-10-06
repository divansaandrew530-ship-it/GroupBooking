package com.ruzinn.serviceimpl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.ruzinn.dto.Experience;
import com.ruzinn.dto.ParkDayPlan;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.RoomRate;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.CmsTranslationService;
import com.ruzinn.service.ItemMasterMediaStore;
import com.ruzinn.service.ParkCatalogService;
import com.ruzinn.service.ParkQuotePdfService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParkQuotePdfServiceImpl implements ParkQuotePdfService {

  private static final Logger log = LoggerFactory.getLogger(ParkQuotePdfServiceImpl.class);

  private static final Color BRAND = new Color(0x07, 0x58, 0xB7);
  private static final Color INK = new Color(0x14, 0x25, 0x3A);
  private static final Color MUTED = new Color(0x64, 0x75, 0x8B);
  private static final Color ACCENT = new Color(0x0F, 0x8C, 0xCB);
  private static final Color LINE = new Color(226, 232, 240);
  private static final Color BAND = new Color(241, 245, 249);
  private static final Color PALE = new Color(0xF2, 0xF7, 0xFC);
  private static final Color CARD_STROKE = new Color(0xD8, 0xE3, 0xEF);
  private static final Color DISCLAIMER = new Color(248, 250, 252);
  private static final Color HEADER_SUB = new Color(0xDC, 0xEB, 0xFF);
  private static BaseFont holidaySans;
  private static BaseFont holidaySansBold;

  private final ItemMasterRepository itemMasterRepository;
  private final ItemMasterMediaStore mediaStore;
  private final ParkCatalogService catalogService;
  private final CmsTranslationService translations;
  private final AccountService accountService;

  public ParkQuotePdfServiceImpl(
      ItemMasterRepository itemMasterRepository,
      ItemMasterMediaStore mediaStore,
      ParkCatalogService catalogService,
      CmsTranslationService translations,
      AccountService accountService) {
    this.itemMasterRepository = itemMasterRepository;
    this.mediaStore = mediaStore;
    this.catalogService = catalogService;
    this.translations = translations;
    this.accountService = accountService;
  }

  @Override
  public byte[] render(ParkQuote quote) {
    log.debug(
        "Rendering {} PDF for quote {}",
        quote != null && quote.isDirectCustomer() ? "holiday package" : "B2B quotation",
        quote == null ? "-" : quote.getQuoteNumber());
    if (quote != null && quote.isDirectCustomer()) {
      return renderHolidayPackage(quote);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 42, 42, 42, 48);
    PdfWriter writer = PdfWriter.getInstance(document, out);
    writer.setCompressionLevel(0);
    document.open();
    document.addTitle(quote.getQuoteNumber() + " Rathin quotation");
    document.addAuthor(quote.getBrandName());
    document.addCreator("GroupBookingLA");

    Font brandFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE);
    Font brandSmall = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(219, 234, 254));
    Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, INK);
    Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, MUTED);
    Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK);
    Font strongFont = new Font(Font.HELVETICA, 10, Font.BOLD, INK);
    Font moneyFont = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND);
    Font totalFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);

    PdfPTable header = new PdfPTable(2);
    header.setWidthPercentage(100);
    header.setWidths(new float[] {58, 42});
    PdfPCell brandCell = cell(new Phrase(), Rectangle.NO_BORDER, BRAND);
    brandCell.setPadding(14);
    Image logo = brandLogoImage(quote, 150, 42);
    if (logo != null) {
      brandCell.addElement(logo);
    }
    Paragraph brand = new Paragraph(quote.getBrandName(), brandFont);
    brand.add(Chunk.NEWLINE);
    brand.add(new Chunk("GroupBookingLA quotation", brandSmall));
    if (hasText(quote.getBrandContact())) {
      brand.add(Chunk.NEWLINE);
      brand.add(new Chunk(quote.getBrandContact(), brandSmall));
    }
    brandCell.addElement(brand);
    header.addCell(brandCell);

    PdfPCell metaCell = cell(new Phrase(), Rectangle.NO_BORDER, BRAND);
    metaCell.setPadding(14);
    metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Paragraph meta = new Paragraph("QUOTATION", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE));
    meta.setAlignment(Element.ALIGN_RIGHT);
    meta.add(Chunk.NEWLINE);
    meta.add(new Chunk(nullToDash(quote.getQuoteNumber()), new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE)));
    meta.add(Chunk.NEWLINE);
    meta.add(new Chunk("Issued " + nullToDash(quote.getIssuedOn()), brandSmall));
    meta.setAlignment(Element.ALIGN_RIGHT);
    metaCell.addElement(meta);
    header.addCell(metaCell);
    document.add(header);

    document.add(space(12));
    document.add(new Paragraph("Quoted itinerary", titleFont));
    document.add(space(4));
    Paragraph intro = new Paragraph();
    intro.add(new Chunk("Prepared by ", bodyFont));
    intro.add(new Chunk(nullToDash(quote.getPreparedBy()), strongFont));
    if (quote.getCompanyName() != null && !quote.getCompanyName().isBlank()) {
      intro.add(new Chunk("  ·  " + quote.getCompanyName(), bodyFont));
    }
    intro.add(Chunk.NEWLINE);
    intro.add(new Chunk(quote.getValidityCopy(), new Font(Font.HELVETICA, 9, Font.ITALIC, MUTED)));
    document.add(intro);
    document.add(space(10));

    PdfPTable table = new PdfPTable(4);
    table.setWidthPercentage(100);
    table.setWidths(new float[] {36, 26, 18, 20});
    table.addCell(headerCell("Item", labelFont));
    table.addCell(headerCell("Details", labelFont));
    table.addCell(headerCell("Guests", labelFont));
    table.addCell(headerCell(amountHeader(quote.getCurrency()), labelFont, Element.ALIGN_RIGHT));

    if (quote.getItems() != null) {
      for (ParkQuoteLine line : quote.getItems()) {
        table.addCell(bodyCell(line.getName() + "\n" + line.getKindLabel(), bodyFont, strongFont));
        table.addCell(bodyCell(detailsFor(line), bodyFont, null));
        table.addCell(bodyCell(line.getGuestSummary(), bodyFont, null));
        table.addCell(bodyCell(money(line.getLineTotal()), moneyFont, null, Element.ALIGN_RIGHT));
      }
    }
    document.add(table);
    document.add(space(12));

    PdfPTable totals = new PdfPTable(2);
    totals.setWidthPercentage(46);
    totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
    totals.addCell(totalLabel("Subtotal", bodyFont));
    totals.addCell(totalValue(money(quote.getSubtotal()), strongFont, false));
    totals.addCell(totalLabel("Platform charge (" + stripZeros(quote.getMarkupPercent()) + "%)", bodyFont));
    totals.addCell(totalValue(money(quote.getMarkupAmount()), strongFont, false));
    PdfPCell grandLabel = totalLabel("Grand total", totalFont);
    grandLabel.setBackgroundColor(BRAND);
    grandLabel.setPadding(9);
    totals.addCell(grandLabel);
    PdfPCell grandValue = totalValue(money(quote.getTotal()), totalFont, true);
    grandValue.setBackgroundColor(BRAND);
    grandValue.setPadding(9);
    totals.addCell(grandValue);
    document.add(totals);

    if (quote.isHasOnRequest()) {
      document.add(space(8));
      document.add(new Paragraph("On-request tickets are shown at AED 0 until the supplier confirms a rate.", new Font(Font.HELVETICA, 9, Font.ITALIC, MUTED)));
    }

    document.add(space(18));
    Paragraph foot = new Paragraph(quote.getBrandFooter() + "  ·  Quote " + nullToDash(quote.getQuoteNumber()), new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED));
    document.add(foot);
    appendItemDetailPages(document, quote);
    document.close();
    return out.toByteArray();
  }

  private byte[] renderHolidayPackage(ParkQuote quote) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 48, 48, 148, 42);
    PdfWriter writer = PdfWriter.getInstance(document, out);
    writer.setCompressionLevel(0);
    writer.setPageEvent(new HolidayChrome(quote, brandLogoBytes(quote)));
    document.open();
    document.addTitle(quote.getQuoteNumber() + " holiday package quotation");
    document.addAuthor(quote.getBrandName());
    document.addCreator("GroupBookingLA");
    writeHolidayCover(document, quote);
    document.newPage();
    writeHolidayItinerary(document, quote);
    writeHolidayNotes(document, quote);
    document.close();
    return out.toByteArray();
  }

  private static void writeHolidayCover(Document document, ParkQuote quote) {
    Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, INK);
    Font kickerFont = new Font(Font.HELVETICA, 8, Font.BOLD, MUTED);
    Font bodyFont = new Font(Font.HELVETICA, 10.5f, Font.NORMAL, INK);
    Font mutedFont = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
    Font strongFont = new Font(Font.HELVETICA, 13, Font.BOLD, INK);
    Font hotelFont = new Font(Font.HELVETICA, 14, Font.BOLD, INK);
    Font durationFont = new Font(Font.HELVETICA, 11, Font.BOLD, ACCENT);
    Font guestCountFont = new Font(Font.HELVETICA, 18, Font.BOLD, BRAND);
    Font moneyFont = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND);
    Font noteFont = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);

    Paragraph title = new Paragraph(blankTo(quote.getPackageTitle(), "Holiday Package"), titleFont);
    title.setSpacingAfter(8);
    document.add(title);

    PdfPTable meta = new PdfPTable(3);
    meta.setWidthPercentage(100);
    meta.setWidths(new float[] {22, 50, 28});
    meta.setSpacingAfter(14);
    meta.addCell(plainCell(quote.getDurationPhrase(), durationFont, Element.ALIGN_LEFT));
    meta.addCell(plainCell(quote.getPrintDateRange(), mutedFont, Element.ALIGN_LEFT));
    meta.addCell(plainCell(quote.getGuestLabel(), strongFont, Element.ALIGN_RIGHT));
    document.add(meta);

    PdfPTable split = new PdfPTable(3);
    split.setWidthPercentage(100);
    split.setWidths(new float[] {48, 4, 48});
    split.setSpacingAfter(10);
    split.addCell(roundedCard(customerCard(quote, kickerFont, strongFont, bodyFont), Color.WHITE, CARD_STROKE, 8, 96));
    split.addCell(plainCell("", bodyFont, Element.ALIGN_LEFT));
    split.addCell(
        roundedCard(
            guestsCard(quote, kickerFont, guestCountFont, mutedFont), Color.WHITE, CARD_STROKE, 8, 96));
    document.add(split);

    PdfPTable stay = new PdfPTable(1);
    stay.setWidthPercentage(100);
    stay.setSpacingAfter(10);
    stay.addCell(roundedCard(accommodationCard(quote, kickerFont, hotelFont, mutedFont), PALE, CARD_STROKE, 8, 86));
    document.add(stay);

    PdfPTable price = new PdfPTable(1);
    price.setWidthPercentage(100);
    price.setSpacingAfter(16);
    price.addCell(roundedCard(priceCard(quote, kickerFont, moneyFont, noteFont), Color.WHITE, CARD_STROKE, 8, 84));
    document.add(price);

    PdfPTable rule = new PdfPTable(1);
    rule.setWidthPercentage(100);
    PdfPCell line = cell(new Phrase(" "), Rectangle.BOTTOM, Color.WHITE);
    line.setBorderColor(LINE);
    line.setPaddingBottom(8);
    line.setFixedHeight(10);
    rule.addCell(line);
    document.add(rule);

    PdfPTable prepared = new PdfPTable(1);
    prepared.setWidthPercentage(100);
    prepared.setSpacingBefore(8);
    prepared.setSpacingAfter(12);
    Phrase preparedPhrase = new Phrase();
    preparedPhrase.add(new Chunk("PREPARED BY    ", kickerFont));
    preparedPhrase.add(
        new Chunk(preparedByLine(quote), new Font(Font.HELVETICA, 11, Font.BOLD, INK)));
    prepared.addCell(plainCell(preparedPhrase, Element.ALIGN_LEFT));
    if (hasText(quote.getBrandContact())) {
      prepared.addCell(plainCell(new Phrase(quote.getBrandContact(), mutedFont), Element.ALIGN_LEFT));
    }
    document.add(prepared);

    PdfPTable disclaimer = new PdfPTable(1);
    disclaimer.setWidthPercentage(100);
    disclaimer.addCell(
        roundedCard(
            paddedPhrase(
                quote.getValidityCopy(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED)),
            DISCLAIMER,
            DISCLAIMER,
            8));
    document.add(disclaimer);
  }

  private static void writeHolidayItinerary(Document document, ParkQuote quote) {
    Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, INK);
    Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, MUTED);
    Font dateFont = new Font(Font.HELVETICA, 9, Font.BOLD, ACCENT);
    Font dayTitleFont = new Font(Font.HELVETICA, 12, Font.BOLD, INK);
    Font noteFont = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, MUTED);

    Paragraph heading = new Paragraph("Day-by-Day Itinerary", titleFont);
    heading.setSpacingAfter(4);
    document.add(heading);
    Paragraph subtitle = new Paragraph(blankTo(quote.getPackageTitle(), "Holiday Package"), subtitleFont);
    subtitle.setSpacingAfter(12);
    document.add(subtitle);

    List<ParkDayPlan> plans = quote.getDayPlans() == null ? List.of() : quote.getDayPlans();
    int dayNumber = 1;
    for (ParkDayPlan plan : plans) {
      if (plan == null) {
        continue;
      }
      document.add(dayCard(dayNumber, plan, dateFont, dayTitleFont, noteFont));
      dayNumber++;
    }
    if (plans.isEmpty()) {
      PdfPTable empty = new PdfPTable(1);
      empty.setWidthPercentage(100);
      empty.addCell(
          roundedCard(
              paddedPhrase("Day-by-day details will be confirmed with the guest.", noteFont),
              Color.WHITE,
              CARD_STROKE,
              8));
      document.add(empty);
    }
  }

  private static void writeHolidayNotes(Document document, ParkQuote quote) {
    boolean hasNotes =
        hasText(quote.getPackageInclusions())
            || hasText(quote.getPackageExclusions())
            || hasText(quote.getGuestInformation());
    if (!hasNotes) {
      document.add(packageNoteCard("Package Inclusions", "To be advised."));
      document.add(packageNoteCard("Package Exclusions", "To be advised."));
      document.add(packageNoteCard("Important Guest Information", "To be advised."));
      return;
    }
    document.newPage();
    Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, INK);
    Paragraph heading = new Paragraph("Package notes", titleFont);
    heading.setSpacingAfter(12);
    document.add(heading);
    document.add(
        packageNoteCard(
            "Package Inclusions",
            hasText(quote.getPackageInclusions()) ? quote.getPackageInclusions() : "To be advised."));
    document.add(
        packageNoteCard(
            "Package Exclusions",
            hasText(quote.getPackageExclusions()) ? quote.getPackageExclusions() : "To be advised."));
    document.add(
        packageNoteCard(
            "Important Guest Information",
            hasText(quote.getGuestInformation()) ? quote.getGuestInformation() : "To be advised."));
  }

  private static PdfPTable dayCard(
      int dayNumber, ParkDayPlan plan, Font dateFont, Font titleFont, Font noteFont) {
    PdfPTable card = new PdfPTable(1);
    card.setWidthPercentage(100);
    card.setSpacingAfter(10);
    card.setKeepTogether(true);

    Paragraph body = new Paragraph();
    if (hasText(plan.getDate())) {
      body.add(new Chunk(ParkQuoteLine.formatDate(plan.getDate()), dateFont));
      body.add(Chunk.NEWLINE);
    }
    if (hasText(plan.getTitle())) {
      body.add(new Chunk(plan.getTitle().trim(), titleFont));
      body.add(Chunk.NEWLINE);
    }
    if (hasText(plan.getNote())) {
      body.add(new Chunk(normalizeText(plan.getNote()), noteFont));
    }
    body.setLeading(13.5f);
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPaddingTop(12);
    cell.setPaddingBottom(12);
    cell.setPaddingRight(14);
    cell.setPaddingLeft(72);
    cell.addElement(body);
    cell.setCellEvent(new DayCardChrome("Day " + dayNumber));
    card.addCell(cell);
    return card;
  }

  private static PdfPTable packageNoteCard(String heading, String body) {
    Font kicker = new Font(Font.HELVETICA, 11, Font.BOLD, INK);
    Font copy = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
    Paragraph content = new Paragraph();
    content.add(new Chunk(heading, kicker));
    content.add(Chunk.NEWLINE);
    content.add(new Chunk(normalizeText(body), copy));
    content.setLeading(14);
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingAfter(10);
    table.setKeepTogether(true);
    table.addCell(roundedCard(content, Color.WHITE, CARD_STROKE, 8));
    return table;
  }

  private static Paragraph customerCard(ParkQuote quote, Font kicker, Font nameFont, Font bodyFont) {
    Paragraph content = new Paragraph();
    content.add(new Chunk("CUSTOMER", kicker));
    content.add(Chunk.NEWLINE);
    String name = quote.getCustomerName();
    content.add(new Chunk(hasText(name) ? name.trim() : "—", nameFont));
    if (hasText(quote.getCustomerEmail())) {
      content.add(Chunk.NEWLINE);
      content.add(new Chunk(quote.getCustomerEmail().trim(), bodyFont));
    }
    if (hasText(quote.getCustomerPhone())) {
      content.add(Chunk.NEWLINE);
      content.add(new Chunk(quote.getCustomerPhone().trim(), bodyFont));
    }
    content.setLeading(16);
    return content;
  }

  private static Paragraph guestsCard(ParkQuote quote, Font kicker, Font countFont, Font mutedFont) {
    Paragraph content = new Paragraph();
    content.add(new Chunk("GUESTS", kicker));
    content.add(Chunk.NEWLINE);
    content.add(new Chunk(quote.getGuestLabel(), countFont));
    content.add(Chunk.NEWLINE);
    content.add(new Chunk("Private holiday package", mutedFont));
    content.setLeading(18);
    return content;
  }

  private static Paragraph accommodationCard(ParkQuote quote, Font kicker, Font nameFont, Font mutedFont) {
    Paragraph content = new Paragraph();
    content.add(new Chunk("ACCOMMODATION", kicker));
    content.add(Chunk.NEWLINE);
    content.add(new Chunk(hasText(quote.getHotelName()) ? quote.getHotelName() : "To be advised", nameFont));
    if (hasText(quote.getStayDetailLine())) {
      content.add(Chunk.NEWLINE);
      content.add(new Chunk(quote.getStayDetailLine(), mutedFont));
    }
    content.setLeading(16);
    return content;
  }

  private static PdfPTable priceCard(ParkQuote quote, Font kicker, Font moneyFont, Font noteFont) {
    PdfPTable inner = new PdfPTable(2);
    inner.setWidthPercentage(100);
    inner.setWidths(new float[] {66, 34});
    Paragraph amount = new Paragraph();
    amount.add(new Chunk("PACKAGE PRICE", kicker));
    amount.add(Chunk.NEWLINE);
    amount.add(new Chunk(quote.getPackageAmountLabel(), moneyFont));
    amount.setLeading(16);
    PdfPCell left = new PdfPCell();
    left.setBorder(Rectangle.NO_BORDER);
    left.addElement(amount);
    Paragraph notes = new Paragraph();
    notes.add(new Chunk("Total package amount", noteFont));
    notes.add(Chunk.NEWLINE);
    notes.add(new Chunk("Inclusive of listed arrangements", noteFont));
    notes.setAlignment(Element.ALIGN_RIGHT);
    notes.setLeading(12);
    PdfPCell right = new PdfPCell();
    right.setBorder(Rectangle.NO_BORDER);
    right.setVerticalAlignment(Element.ALIGN_MIDDLE);
    right.addElement(notes);
    inner.addCell(left);
    inner.addCell(right);
    return inner;
  }

  private static String preparedByLine(ParkQuote quote) {
    String name = hasText(quote.getPreparedBy()) ? quote.getPreparedBy().trim() : "Rathin";
    if (hasText(quote.getCompanyName())) {
      return name + " - " + quote.getCompanyName().trim();
    }
    return name;
  }

  private static PdfPCell roundedCard(Object content, Color fill, Color stroke, float radius) {
    return roundedCard(content, fill, stroke, radius, 0);
  }

  private static PdfPCell roundedCard(Object content, Color fill, Color stroke, float radius, float minHeight) {
    PdfPCell cell;
    if (content instanceof PdfPTable table) {
      cell = new PdfPCell(table);
    } else if (content instanceof Phrase phrase) {
      cell = new PdfPCell(phrase);
    } else if (content instanceof Paragraph paragraph) {
      cell = new PdfPCell();
      cell.addElement(paragraph);
    } else {
      cell = new PdfPCell(new Phrase(String.valueOf(content)));
    }
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setCellEvent(new RoundedCard(fill, stroke, radius));
    cell.setPadding(14);
    cell.setPaddingLeft(16);
    cell.setPaddingRight(16);
    if (minHeight > 0) {
      cell.setMinimumHeight(minHeight);
    }
    return cell;
  }

  private static Paragraph paddedPhrase(String text, Font font) {
    Paragraph paragraph = new Paragraph(text, font);
    paragraph.setLeading(13);
    return paragraph;
  }

  private static PdfPCell plainCell(Phrase phrase, int align) {
    PdfPCell cell = cell(phrase, Rectangle.NO_BORDER, Color.WHITE);
    cell.setHorizontalAlignment(align);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setPadding(0);
    cell.setPaddingBottom(2);
    return cell;
  }

  private static PdfPCell plainCell(String text, Font font, int align) {
    return plainCell(new Phrase(text == null ? "" : text, font), align);
  }

  private static final class RoundedCard implements PdfPCellEvent {
    private final Color fill;
    private final Color stroke;
    private final float radius;

    private RoundedCard(Color fill, Color stroke, float radius) {
      this.fill = fill;
      this.stroke = stroke;
      this.radius = radius;
    }

    @Override
    public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
      paintRoundRect(canvases, rect, fill, stroke, radius);
    }
  }

  private static final class DayCardChrome implements PdfPCellEvent {
    private static final float CHIP_H = 18f;
    private static final float CHIP_RADIUS = 5.5f;
    private static final float CHIP_FONT = 8f;
    private final String label;

    private DayCardChrome(String label) {
      this.label = label;
    }

    @Override
    public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
      paintRoundRect(canvases, rect, Color.WHITE, CARD_STROKE, 8f);
      BaseFont font = holidayBold();
      float textW = font.getWidthPoint(label, CHIP_FONT);
      float chipW = Math.max(46f, textW + 16f);
      float x = rect.getLeft() + 14f;
      float y = rect.getTop() - 14f - CHIP_H;
      PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];
      PdfTemplate chip = canvas.createTemplate(chipW, CHIP_H);
      chip.setColorFill(BRAND);
      chip.roundRectangle(0, 0, chipW, CHIP_H, CHIP_RADIUS);
      chip.fill();
      chip.beginText();
      chip.setColorFill(Color.WHITE);
      chip.setFontAndSize(font, CHIP_FONT);
      chip.setTextMatrix((chipW - textW) / 2f, 5.2f);
      chip.showText(label);
      chip.endText();
      canvas.addTemplate(chip, x, y);
    }
  }

  private static void paintRoundRect(
      PdfContentByte[] canvases, Rectangle rect, Color fill, Color stroke, float radius) {
    float x = rect.getLeft() + 0.6f;
    float y = rect.getBottom() + 0.6f;
    float w = rect.getWidth() - 1.2f;
    float h = rect.getHeight() - 1.2f;
    if (fill != null) {
      PdfContentByte background = canvases[PdfPTable.BACKGROUNDCANVAS];
      background.saveState();
      background.roundRectangle(x, y, w, h, radius);
      background.setColorFill(fill);
      background.fill();
      background.restoreState();
    }
    if (stroke != null) {
      PdfContentByte line = canvases[PdfPTable.LINECANVAS];
      line.saveState();
      line.roundRectangle(x, y, w, h, radius);
      line.setColorStroke(stroke);
      line.setLineWidth(0.9f);
      line.stroke();
      line.restoreState();
    }
  }

  private static BaseFont holidayBold() {
    ensureHolidayFonts();
    return holidaySansBold;
  }

  private static void ensureHolidayFonts() {
    if (holidaySansBold != null && holidaySans != null) {
      return;
    }
    try {
      holidaySans = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
      holidaySansBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not load PDF fonts", ex);
    }
  }

  private static final class HolidayChrome extends PdfPageEventHelper {
    private final String quoteNumber;
    private final String issuedOn;
    private final String brandName;
    private final String footer;
    private final byte[] logoBytes;
    private PdfTemplate totalPages;
    private BaseFont regular;
    private BaseFont bold;

    private HolidayChrome(ParkQuote quote, byte[] logoBytes) {
      this.quoteNumber = nullToDash(quote == null ? null : quote.getQuoteNumber());
      this.issuedOn = nullToDash(quote == null ? null : quote.getIssuedOn());
      this.brandName = quote == null ? "Rathin" : quote.getBrandName();
      this.footer = quote == null ? "Rathin" : quote.getBrandFooter();
      this.logoBytes = logoBytes;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      totalPages = writer.getDirectContent().createTemplate(30, 12);
      ensureHolidayFonts();
      regular = holidaySans;
      bold = holidaySansBold;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      Rectangle page = document.getPageSize();
      PdfContentByte cb = writer.getDirectContent();
      float top = page.getTop();
      float headerHeight = 112f;
      cb.saveState();
      cb.setColorFill(BRAND);
      cb.rectangle(0, top - headerHeight, page.getWidth(), headerHeight);
      cb.fill();
      cb.restoreState();

      float left = 48f;
      float right = page.getWidth() - 48f;
      float textLeft = left;
      if (logoBytes != null && logoBytes.length > 0) {
        try {
          Image logo = Image.getInstance(logoBytes);
          logo.scaleToFit(150f, 48f);
          logo.setAbsolutePosition(left, top - 68);
          cb.addImage(logo);
          textLeft = left + logo.getScaledWidth() + 12;
        } catch (Exception ignored) {
          // fall back to company name text when the stored logo cannot be drawn
        }
      }
      cb.beginText();
      cb.setColorFill(Color.WHITE);
      cb.setFontAndSize(bold, textLeft > left ? 16 : 24);
      cb.setTextMatrix(textLeft, top - 50);
      cb.showText(brandName);
      cb.setColorFill(HEADER_SUB);
      cb.setFontAndSize(bold, 11);
      cb.setTextMatrix(textLeft, top - 77);
      cb.showText("Holiday package quotation");
      cb.setColorFill(Color.WHITE);
      cb.setFontAndSize(bold, 10);
      cb.showTextAligned(Element.ALIGN_RIGHT, "QUOTATION", right, top - 33, 0);
      cb.setFontAndSize(bold, 18);
      cb.showTextAligned(Element.ALIGN_RIGHT, quoteNumber, right, top - 57, 0);
      cb.setColorFill(HEADER_SUB);
      cb.setFontAndSize(bold, 10);
      cb.showTextAligned(Element.ALIGN_RIGHT, "Issued " + issuedOn, right, top - 79, 0);
      cb.endText();

      cb.setColorFill(MUTED);
      cb.beginText();
      cb.setFontAndSize(regular, 8.5f);
      String foot = footer == null || footer.isBlank() ? brandName : footer;
      if (foot.length() > 120) {
        foot = foot.substring(0, 117) + "...";
      }
      cb.showTextAligned(Element.ALIGN_LEFT, foot, left, 30, 0);
      String prefix = "Page " + writer.getPageNumber() + " of ";
      float prefixWidth = regular.getWidthPoint(prefix, 8.5f);
      cb.showTextAligned(Element.ALIGN_LEFT, prefix, right - prefixWidth - 16, 30, 0);
      cb.endText();
      cb.addTemplate(totalPages, right - 16, 30);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      totalPages.beginText();
      totalPages.setFontAndSize(regular, 8.5f);
      totalPages.setColorFill(MUTED);
      totalPages.showText(String.valueOf(Math.max(1, writer.getPageNumber() - 1)));
      totalPages.endText();
    }
  }

  private void appendItemDetailPages(Document document, ParkQuote quote) {
    if (quote == null || quote.getItems() == null || quote.getItems().isEmpty()) {
      return;
    }
    Map<String, ParkQuoteLine> unique = new LinkedHashMap<>();
    for (ParkQuoteLine line : quote.getItems()) {
      if (line == null || !isDetailPageItem(line)) {
        continue;
      }
      unique.putIfAbsent(detailKey(line), line);
    }
    Font kindFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, INK);
    Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
    Font headingFont = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND);
    Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK);
    for (ParkQuoteLine line : unique.values()) {
      document.newPage();
      writeDetailPage(document, resolveDetail(line), kindFont, titleFont, metaFont, headingFont, bodyFont);
    }
  }

  private ItemDetail resolveDetail(ParkQuoteLine line) {
    ItemDetail detail = new ItemDetail();
    detail.title = blankTo(line.getName(), "Item");
    detail.kindLabel = line.getKindLabel();
    detail.category = blankTo(line.getCategory(), "");
    detail.city = blankTo(line.getCity(), "");
    Experience catalog = catalogService.find(line.getItemId()).orElse(null);
    if (catalog != null) {
      if (!hasText(detail.category)) {
        detail.category = blankTo(catalog.getCategory(), "");
      }
      if (!hasText(detail.city)) {
        detail.city = blankTo(catalog.getCity(), "");
      }
      detail.hotelSummary = hotelSummary(catalog);
      detail.roomTypes = roomTypes(catalog);
    }
    ItemMaster item = findMaster(line);
    if (item != null) {
      applyMaster(detail, item);
    }
    overlayLine(detail, line, item);
    return detail;
  }

  private ItemMaster findMaster(ParkQuoteLine line) {
    if (line == null) {
      return null;
    }
    if (hasText(line.getItemId())) {
      ItemMaster byId = itemMasterRepository.findById(line.getItemId().trim()).orElse(null);
      if (byId != null) {
        return byId;
      }
    }
    if (hasText(line.getName())) {
      return itemMasterRepository.findByItemNameIgnoreCase(line.getName().trim()).orElse(null);
    }
    return null;
  }

  private static void overlayLine(ItemDetail detail, ParkQuoteLine line, ItemMaster item) {
    if (line == null) {
      return;
    }
    if (isCustom(line.getDescription(), item == null ? null : item.getDescription())) {
      detail.description = line.getDescription().trim();
    }
    if (isCustom(line.getIncluded(), item == null ? null : item.getIncluded())) {
      detail.included = line.getIncluded().trim();
    }
    if (isCustom(line.getExcluded(), item == null ? null : item.getExcluded())) {
      detail.excluded = line.getExcluded().trim();
    }
    if (isCustom(line.getTermsAndCondition(), item == null ? null : item.getTermsAndCondition())) {
      detail.terms = line.getTermsAndCondition().trim();
    }
  }

  private void applyMaster(ItemDetail detail, ItemMaster item) {
    String itemId = item.getItemId();
    String name = translations.itemField(itemId, "item_name", item.getItemName());
    if (hasText(name)) {
      detail.title = name;
    }
    if (hasText(item.getCategory())) {
      detail.category = item.getCategory();
    }
    detail.description = textOrKeep(detail.description, translations.itemField(itemId, "description", item.getDescription()));
    detail.included = textOrKeep(detail.included, translations.itemField(itemId, "included", item.getIncluded()));
    detail.excluded = textOrKeep(detail.excluded, translations.itemField(itemId, "excluded", item.getExcluded()));
    detail.terms =
        textOrKeep(
            detail.terms, translations.itemField(itemId, "terms_and_condition", item.getTermsAndCondition()));
    byte[] image = loadEmbeddableImage(item.getImagePdf());
    if (image == null) {
      image = loadEmbeddableImage(item.getImageTicket());
    }
    if (image != null) {
      detail.image = image;
    }
  }

  private void writeDetailPage(
      Document document,
      ItemDetail detail,
      Font kindFont,
      Font titleFont,
      Font metaFont,
      Font headingFont,
      Font bodyFont) {
    PdfPTable banner = new PdfPTable(1);
    banner.setWidthPercentage(100);
    PdfPCell bannerCell = cell(new Phrase(), Rectangle.NO_BORDER, BRAND);
    bannerCell.setPadding(12);
    Paragraph bannerText = new Paragraph(detail.kindLabel.toUpperCase(Locale.US) + " DETAILS", kindFont);
    bannerCell.addElement(bannerText);
    banner.addCell(bannerCell);
    document.add(banner);

    document.add(space(12));
    document.add(new Paragraph(detail.title, titleFont));
    String meta = joinMeta(detail.category, detail.city, detail.hotelSummary);
    if (hasText(meta)) {
      document.add(space(3));
      document.add(new Paragraph(meta, metaFont));
    }

    if (detail.image != null && detail.image.length > 0) {
      try {
        Image photo = Image.getInstance(detail.image);
        photo.scaleToFit(510, 280);
        photo.setAlignment(Element.ALIGN_CENTER);
        photo.setSpacingBefore(12);
        photo.setSpacingAfter(6);
        document.add(photo);
      } catch (Exception ignored) {
        // Skip images OpenPDF cannot embed (for example WebP).
      }
    }

    addSection(document, "Description", detail.description, headingFont, bodyFont);
    addSection(document, "Included", detail.included, headingFont, bodyFont);
    addSection(document, "Excluded", detail.excluded, headingFont, bodyFont);
    addSection(document, "Terms & conditions", detail.terms, headingFont, bodyFont);
    addSection(document, "Room types", detail.roomTypes, headingFont, bodyFont);
  }

  private static void addSection(Document document, String heading, String body, Font headingFont, Font bodyFont) {
    if (!hasText(body)) {
      return;
    }
    document.add(space(12));
    document.add(new Paragraph(heading, headingFont));
    document.add(space(3));
    Paragraph paragraph = new Paragraph(normalizeText(body), bodyFont);
    paragraph.setLeading(14);
    document.add(paragraph);
  }

  private byte[] loadEmbeddableImage(String relativePath) {
    if (!mediaStore.exists(relativePath)) {
      return null;
    }
    String path = relativePath.toLowerCase(Locale.ROOT);
    if (path.endsWith(".webp") || "image/webp".equals(mediaStore.contentType(relativePath))) {
      return null;
    }
    try {
      byte[] bytes = mediaStore.read(relativePath);
      Image.getInstance(bytes);
      return bytes;
    } catch (Exception ex) {
      return null;
    }
  }

  private static String hotelSummary(Experience catalog) {
    if (catalog == null || !"stay".equalsIgnoreCase(catalog.getKind())) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    if (catalog.getStarCategory() != null && catalog.getStarCategory() > 0) {
      parts.add(catalog.getStarCategory() + "-star");
    }
    if (hasText(catalog.getPropertyType())) {
      parts.add(catalog.getPropertyType());
    } else {
      parts.add("Hotel");
    }
    if (hasText(catalog.getCity())) {
      parts.add("in " + catalog.getCity().trim());
    }
    return String.join(" ", parts);
  }

  private static String roomTypes(Experience catalog) {
    if (catalog == null || catalog.getRoomRates() == null || catalog.getRoomRates().isEmpty()) {
      return "";
    }
    List<String> names = new ArrayList<>();
    for (RoomRate rate : catalog.getRoomRates()) {
      if (rate == null || !hasText(rate.getName())) {
        continue;
      }
      String label = rate.getName().trim();
      names.add(label);
    }
    return String.join("\n", names);
  }

  private static boolean isDetailPageItem(ParkQuoteLine line) {
    return line.isPark() || line.isStay();
  }

  private static String detailKey(ParkQuoteLine line) {
    String name = line.getName() == null ? "" : line.getName().trim().toLowerCase(Locale.ROOT);
    String kind = line.getKind() == null ? "" : line.getKind().trim().toLowerCase(Locale.ROOT);
    String itemId = line.getItemId() == null ? "" : line.getItemId().trim().toLowerCase(Locale.ROOT);
    return kind + "|" + (hasText(itemId) ? itemId : name);
  }

  private static String joinMeta(String category, String city, String hotelSummary) {
    List<String> parts = new ArrayList<>();
    if (hasText(category) && !"Hotels".equalsIgnoreCase(category.trim())) {
      parts.add(category.trim());
    }
    if (hasText(hotelSummary)) {
      parts.add(hotelSummary.trim());
    } else if (hasText(city)) {
      parts.add(city.trim());
    }
    return String.join("  ·  ", parts);
  }

  private static String textOrKeep(String current, String incoming) {
    return hasText(incoming) ? incoming.trim() : current;
  }

  private static String normalizeText(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n').trim();
  }

  private byte[] brandLogoBytes(ParkQuote quote) {
    if (quote == null || !quote.isHasLogo()) {
      return null;
    }
    return accountService.logoBytes(accountService.brandingForCompany(quote.getCompanyId()));
  }

  private Image brandLogoImage(ParkQuote quote, float width, float height) {
    byte[] bytes = brandLogoBytes(quote);
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try {
      Image logo = Image.getInstance(bytes);
      logo.scaleToFit(width, height);
      logo.setAlignment(Element.ALIGN_LEFT);
      return logo;
    } catch (Exception ex) {
      return null;
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static boolean isCustom(String lineValue, String masterValue) {
    if (!hasText(lineValue)) {
      return false;
    }
    if (!hasText(masterValue)) {
      return true;
    }
    return !lineValue.trim().equals(masterValue.trim());
  }

  private static String blankTo(String value, String fallback) {
    return hasText(value) ? value.trim() : fallback;
  }

  private static final class ItemDetail {
    private String title = "";
    private String kindLabel = "Item";
    private String category = "";
    private String city = "";
    private String hotelSummary = "";
    private String description = "";
    private String included = "";
    private String excluded = "";
    private String terms = "";
    private String roomTypes = "";
    private byte[] image;
  }

  private static String detailsFor(ParkQuoteLine line) {
    StringBuilder details = new StringBuilder();
    if (line.isStay()) {
      details.append(line.getDisplayVisitDate());
      if (line.getCheckOut() != null) {
        details.append(" → ").append(line.getDisplayCheckOut());
      }
      if (line.getNights() != null) {
        details.append("\n").append(line.getNights()).append(line.getNights() == 1 ? " night" : " nights");
      }
      if (line.getRoomType() != null) {
        details.append("\n").append(line.getRoomType());
        if (line.getRooms() > 0) {
          details.append(" · ").append(line.getRooms()).append(line.getRooms() == 1 ? " room" : " rooms");
        }
      }
    } else {
      details.append(line.getDisplayVisitDate());
      if (line.getTimeSlot() != null && !line.getTimeSlot().isBlank()) {
        details.append("\nTimeslot ").append(line.getTimeSlot());
      }
      details.append("\n").append(line.getTicketSummary());
    }
    return details.toString();
  }

  private static Paragraph space(float leading) {
    Paragraph p = new Paragraph(" ");
    p.setLeading(leading);
    return p;
  }

  private static PdfPCell headerCell(String text, Font font) {
    return headerCell(text, font, Element.ALIGN_LEFT);
  }

  private static PdfPCell headerCell(String text, Font font, int align) {
    PdfPCell cell = cell(new Phrase(text.toUpperCase(Locale.US), font), Rectangle.BOX, BAND);
    cell.setBorderColor(LINE);
    cell.setPadding(8);
    cell.setHorizontalAlignment(align);
    return cell;
  }

  private static PdfPCell bodyCell(String text, Font font, Font firstLine) {
    return bodyCell(text, font, firstLine, Element.ALIGN_LEFT);
  }

  private static PdfPCell bodyCell(String text, Font font, Font firstLine, int align) {
    Phrase phrase = new Phrase();
    String[] parts = (text == null ? "" : text).split("\n", 2);
    phrase.add(new Chunk(parts[0], firstLine != null ? firstLine : font));
    if (parts.length > 1) {
      phrase.add(Chunk.NEWLINE);
      phrase.add(new Chunk(parts[1], font));
    }
    PdfPCell cell = cell(phrase, Rectangle.BOX, Color.WHITE);
    cell.setBorderColor(LINE);
    cell.setPadding(8);
    cell.setHorizontalAlignment(align);
    return cell;
  }

  private static PdfPCell totalLabel(String text, Font font) {
    PdfPCell cell = cell(new Phrase(text, font), Rectangle.NO_BORDER, Color.WHITE);
    cell.setPadding(6);
    return cell;
  }

  private static PdfPCell totalValue(String text, Font font, boolean grand) {
    PdfPCell cell = cell(new Phrase(text, font), Rectangle.NO_BORDER, grand ? BRAND : Color.WHITE);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    cell.setPadding(6);
    return cell;
  }

  private static PdfPCell cell(Phrase phrase, int border, Color background) {
    PdfPCell cell = new PdfPCell(phrase);
    cell.setBorder(border);
    cell.setBackgroundColor(background);
    return cell;
  }

  private static String amountHeader(String currency) {
    return "Amount (" + (currency == null || currency.isBlank() ? "AED" : currency.trim()) + ")";
  }

  private static String money(double amount) {
    NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(amount);
  }

  private static String stripZeros(double value) {
    if (value == (long) value) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  private static String nullToDash(String value) {
    return value == null || value.isBlank() ? "—" : value;
  }
}

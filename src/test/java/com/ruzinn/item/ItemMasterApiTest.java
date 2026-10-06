package com.ruzinn.item;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemMasterApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private javax.sql.DataSource dataSource;

  @Test
  void newItemShowsInBookingCatalogAndCanBeQuoted() throws Exception {
    MockHttpSession superSession = new MockHttpSession();
    mockMvc.perform(apiLogin("Admin@rathin.com", "R@t@2023").session(superSession)).andExpect(status().isOk());
    String itemId =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/admin/items")
                        .session(superSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"itemName":"Brand New Desert Ride","country":"UAE","datedType":"DA",
                             "category":"Ride","adultRate":"250","childRate":"180","bookingInfo":"DFT"}
                            """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.itemId");

    mockMvc
        .perform(get("/api/parks/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.experiences[?(@.id=='%s')].name".formatted(itemId)).value(
            org.hamcrest.Matchers.hasItem("Brand New Desert Ride")))
        .andExpect(jsonPath("$.experiences[?(@.id=='%s')].adultPrice".formatted(itemId)).value(
            org.hamcrest.Matchers.hasItem(250.0)));

    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":2,"children":1,"markupPercent":0,
                     "items":[{"itemId":"%s","visitDate":"2026-10-01","adults":2,"children":1}]}
                    """
                        .formatted(itemId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Brand New Desert Ride"));

    mockMvc.perform(delete("/api/admin/items/" + itemId).session(superSession)).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/parks/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.experiences[?(@.id=='%s')]".formatted(itemId)).isEmpty());
  }

  @Test
  void superAdminCanCrudItemsAndBulkUploadExcel() throws Exception {
    mockMvc.perform(get("/api/admin/items")).andExpect(status().isUnauthorized());

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(superSession))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/admin/items")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "itemName": "XLine Dubai Marina",
                      "country": "UAE",
                      "datedType": "DA",
                      "category": "Ride",
                      "adultRate": "610",
                      "bookingInfo": "ADD_ON",
                      "apiTimeslot": false,
                      "extraFields": ["First Name", "Last Name", "Passport #"],
                      "description": "Zip line over Dubai Marina"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemId").isNotEmpty());

    String created =
        mockMvc
            .perform(get("/api/admin/items").session(superSession).param("q", "XLine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemName").value("XLine Dubai Marina"))
            .andExpect(jsonPath("$[0].country").value("UAE"))
            .andExpect(jsonPath("$[0].datedType").value("DA"))
            .andExpect(jsonPath("$[0].bookingInfo").value("ADD_ON"))
            .andExpect(jsonPath("$[0].extraFields[0]").value("First Name"))
            .andExpect(jsonPath("$[0].adultRate").value(610))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String itemId = com.jayway.jsonpath.JsonPath.read(created, "$[0].itemId");

    mockMvc
        .perform(get("/api/admin/items/" + itemId).session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemName").value("XLine Dubai Marina"));

    mockMvc
        .perform(
            put("/api/admin/items/" + itemId)
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "itemName": "XLine Dubai Marina",
                      "datedType": "DA",
                      "category": "Ride",
                      "adultRate": "620",
                      "childRate": "310",
                      "bookingInfo": "ADD_ON",
                      "apiTimeslot": false,
                      "apiBooking": false,
                      "extraFields": ["First Name", "Last Name", "Passport #"],
                      "description": "Updated zip line"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adultRate").value(620))
        .andExpect(jsonPath("$.childRate").value(310))
        .andExpect(jsonPath("$.description").value("Updated zip line"));

    mockMvc
        .perform(
            multipart("/api/admin/items/" + itemId + "/images")
                .file(new MockMultipartFile("imagePdfFile", "park.jpg", "image/jpeg", jpegBytes()))
                .file(new MockMultipartFile("imageTicketFile", "ticket.png", "image/png", jpegBytes()))
                .session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adultRate").value(620))
        .andExpect(jsonPath("$.description").value("Updated zip line"));

    mockMvc
        .perform(get("/admin/items/" + itemId + "/image/pdf").session(superSession))
        .andExpect(status().isOk())
        .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_TYPE, "image/jpeg"))
        .andExpect(content().bytes(jpegBytes()));

    String withImages =
        mockMvc
            .perform(get("/api/admin/items/" + itemId).session(superSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imagePdf").value(org.hamcrest.Matchers.startsWith("items/" + itemId)))
            .andExpect(jsonPath("$.imageTicket").value(org.hamcrest.Matchers.startsWith("items/" + itemId)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String storedPdf = com.jayway.jsonpath.JsonPath.read(withImages, "$.imagePdf");
    org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.isRegularFile(java.nio.file.Path.of("target/test-uploads").resolve(storedPdf)))
        .isTrue();

    mockMvc
        .perform(get("/api/admin/audit/items/" + itemId).session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("ITEM_MASTER"))
        .andExpect(jsonPath("$.title").value("XLine Dubai Marina"))
        .andExpect(jsonPath("$.archivedRowCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.edits.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    byte[] template =
        mockMvc
            .perform(get("/api/admin/items/template").session(superSession))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("park-items-template.xlsx")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    org.assertj.core.api.Assertions.assertThat(template.length).isGreaterThan(1000);

    MockMultipartFile excel =
        new MockMultipartFile(
            "file",
            "park-items.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            sampleWorkbook());
    mockMvc
        .perform(multipart("/api/admin/items/upload").file(excel).session(superSession))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/admin/items").session(superSession).param("q", "AYA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].itemName").value("AYA Universe (Dubai)"))
        .andExpect(jsonPath("$[0].category").value("Adventure"))
        .andExpect(jsonPath("$[0].country").value("UAE"))
        .andExpect(jsonPath("$[0].datedType").value("DA"))
        .andExpect(jsonPath("$[0].apiTimeslot").value(false))
        .andExpect(jsonPath("$[0].adultRate").value(95));

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from item_master", Number.class).intValue())
        .isGreaterThanOrEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from item_master_hstry where item_id = ?", Number.class, itemId)
                .intValue())
        .isGreaterThanOrEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                    "select archive_action from item_master_hstry where item_id = ? order by hstry_id",
                    String.class,
                    itemId))
        .contains("CREATE", "ITEM_SAVE");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from item_master_hstry where item_id = ? and archive_action = 'ITEM_SAVE' and adult_rate = 610",
                    Number.class,
                    itemId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_type = 'ITEM_MASTER' and entity_id = ?",
                    Number.class,
                    itemId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_type = 'ITEM_MASTER' and entity_id = ? and field_name = 'field1' and new_value = 'First Name'",
                    Number.class,
                    itemId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_type = 'ITEM_MASTER' and entity_id = ? and field_name = 'adult_rate' and old_value = '610' and new_value = '620'",
                    Number.class,
                    itemId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_type = 'ITEM_MASTER' and entity_id = ? and field_name = 'image_pdf' and new_value like 'items/%'",
                    Number.class,
                    itemId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from item_master_hstry where item_name = 'AYA Universe (Dubai)'",
                    Number.class)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_type = 'ITEM_MASTER' and action = 'ITEM_IMPORT' and new_value = 'AYA Universe (Dubai)'",
                    Number.class)
                .intValue())
        .isGreaterThan(0);

    mockMvc
        .perform(delete("/api/admin/items/" + itemId).session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("deleted"));
    mockMvc
        .perform(get("/api/admin/audit/items/" + itemId).session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("XLine Dubai Marina"))
        .andExpect(jsonPath("$.archivedRowCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                    "select archive_action from item_master_hstry where item_id = ? order by hstry_id",
                    String.class,
                    itemId))
        .contains("CREATE", "ITEM_SAVE", "ITEM_DELETE");
    mockMvc
        .perform(get("/api/admin/items/" + itemId).session(superSession))
        .andExpect(status().isBadRequest());

    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Item Scope Co",
                          "userName": "Item Scope Admin",
                          "email": "items.scope@example.test",
                          "phone": "+971500000555",
                          "password": "ItemPass12"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    MockHttpSession companySession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"items.scope@example.test\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/admin/items").session(companySession))
        .andExpect(status().isUnauthorized());
  }

  private static byte[] sampleWorkbook() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Parks");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("Experience / Ticket");
      header.createCell(1).setCellValue("Dated Type");
      header.createCell(2).setCellValue("Category");
      header.createCell(3).setCellValue("API TimeSlot");
      header.createCell(4).setCellValue("API Booking");
      header.createCell(5).setCellValue("Adult");
      header.createCell(6).setCellValue("Child");
      header.createCell(7).setCellValue("Booking_info");
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("AYA Universe (Dubai)");
      row.createCell(1).setCellValue("DA");
      row.createCell(2).setCellValue("Adventure");
      row.createCell(3).setCellValue("N");
      row.createCell(4).setCellValue("N");
      row.createCell(5).setCellValue(95);
      row.createCell(7).setCellValue("DFT");
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private static byte[] jpegBytes() {
    return new byte[] {
      (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07,
      0x06, 0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B,
      0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C,
      0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
      0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
      (byte) 0xFF, (byte) 0xD9
    };
  }
}

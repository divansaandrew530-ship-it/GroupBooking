package com.ruzinn.parks;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruzinn.entity.ItemMaster;
import com.ruzinn.repository.ItemMasterRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import javax.sql.DataSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ParkQuoteContentEditTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DataSource dataSource;
  @Autowired private ItemMasterRepository items;

  @Test
  void editAndQuoteSavesContentRatesHistoryAndPdf() throws Exception {
    upsertMaster(
        "ITMTESTFRAME002",
        "Dubai Frame",
        "MASTER-DESC-SHOULD-NOT-WIN",
        "MASTER-INCLUDED",
        "MASTER-EXCLUDED",
        "MASTER-TERMS");

    MockHttpSession session = adminSession();
    String created =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":1,"markupPercent":0,"items":[
                          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":1,
                           "adultRate":99.5,"childRate":40,
                           "description":"CUSTOM-DESC-DUBAI-FRAME",
                           "termsAndCondition":"CUSTOM-TERMS-DUBAI-FRAME",
                           "included":"CUSTOM-INCLUDED-DUBAI-FRAME",
                           "excluded":"CUSTOM-EXCLUDED-DUBAI-FRAME"}
                        ]}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteNumber").exists())
            .andExpect(jsonPath("$.items[0].description").value("CUSTOM-DESC-DUBAI-FRAME"))
            .andExpect(jsonPath("$.items[0].adultPrice").value(45.0))
            .andExpect(jsonPath("$.items[0].actualAdultPrice").value(45.0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(created, "$.quoteNumber");

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select description from quote_details where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("CUSTOM-DESC-DUBAI-FRAME");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select terms_and_condition from quote_details where quote_ref_num = ?",
                String.class,
                quoteNumber))
        .isEqualTo("CUSTOM-TERMS-DUBAI-FRAME");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select included from quote_details where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("CUSTOM-INCLUDED-DUBAI-FRAME");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select excluded from quote_details where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("CUSTOM-EXCLUDED-DUBAI-FRAME");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select adult_rate from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(45.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select child_rate from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(25.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select line_total from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(70.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_adult_rate from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(45.0);

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.details[0].description").value("CUSTOM-DESC-DUBAI-FRAME"))
        .andExpect(jsonPath("$.details[0].termsAndCondition").value("CUSTOM-TERMS-DUBAI-FRAME"))
        .andExpect(jsonPath("$.details[0].included").value("CUSTOM-INCLUDED-DUBAI-FRAME"))
        .andExpect(jsonPath("$.details[0].excluded").value("CUSTOM-EXCLUDED-DUBAI-FRAME"))
        .andExpect(jsonPath("$.details[0].actualAdultRate").value(45.0));

    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":1,"markupPercent":0,"quoteNumber":"%s","items":[
                      {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":1,
                       "adultRate":120,"childRate":50,
                       "description":"UPDATED-DESC-DUBAI-FRAME",
                       "termsAndCondition":"UPDATED-TERMS-DUBAI-FRAME",
                       "included":"UPDATED-INCLUDED-DUBAI-FRAME",
                       "excluded":"UPDATED-EXCLUDED-DUBAI-FRAME"}
                    ]}
                    """
                        .formatted(quoteNumber)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteNumber").value(quoteNumber))
        .andExpect(jsonPath("$.items[0].description").value("UPDATED-DESC-DUBAI-FRAME"));

    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select description from quote_details where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("UPDATED-DESC-DUBAI-FRAME");
    org.assertj.core.api.Assertions.assertThat(
            tableExists(jdbc, "quote_details_hstry"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from quote_details_Hstry where quote_ref_num = ?",
                    Number.class,
                    quoteNumber)
                .intValue())
        .isGreaterThanOrEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_id = ? and action = 'QUOTE_EDIT' and field_name = 'item_content'",
                    Number.class,
                    quoteNumber)
                .intValue())
        .isZero();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "select field_name, old_value, new_value from change_history where entity_id = ? and action = 'QUOTE_EDIT'",
                quoteNumber)
                .stream()
                .map(
                    row -> {
                      java.util.Map<String, String> mapped = new java.util.LinkedHashMap<>();
                      row.forEach((key, value) -> mapped.put(key.toLowerCase(Locale.ROOT), String.valueOf(value)));
                      return mapped;
                    })
                .toList())
        .anySatisfy(
            row -> {
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("field_name")))
                  .contains("description");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("old_value")))
                  .isEqualTo("CUSTOM-DESC-DUBAI-FRAME");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("new_value")))
                  .isEqualTo("UPDATED-DESC-DUBAI-FRAME");
            })
        .anySatisfy(
            row -> {
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("field_name")))
                  .contains("terms_and_condition");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("old_value")))
                  .isEqualTo("CUSTOM-TERMS-DUBAI-FRAME");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("new_value")))
                  .isEqualTo("UPDATED-TERMS-DUBAI-FRAME");
            })
        .anySatisfy(
            row -> {
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("field_name")))
                  .contains("included");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("new_value")))
                  .isEqualTo("UPDATED-INCLUDED-DUBAI-FRAME");
            })
        .anySatisfy(
            row -> {
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("field_name")))
                  .contains("excluded");
              org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("new_value")))
                  .isEqualTo("UPDATED-EXCLUDED-DUBAI-FRAME");
            });
    mockMvc
        .perform(get("/api/company/audit/quotes/" + quoteNumber).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.edits[?(@.from == 'CUSTOM-DESC-DUBAI-FRAME')].to").value(Matchers.hasItem("UPDATED-DESC-DUBAI-FRAME")))
        .andExpect(jsonPath("$.edits[?(@.from == 'CUSTOM-TERMS-DUBAI-FRAME')].to").value(Matchers.hasItem("UPDATED-TERMS-DUBAI-FRAME")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("desc="))))
        .andExpect(content().string(Matchers.not(Matchers.containsString("Quote item content"))));

    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":1,"markupPercent":0,"quoteNumber":"%s","items":[
                          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":1,
                           "adultRate":120,"childRate":50,
                           "description":"UPDATED-DESC-DUBAI-FRAME",
                           "termsAndCondition":"UPDATED-TERMS-DUBAI-FRAME",
                           "included":"UPDATED-INCLUDED-DUBAI-FRAME",
                           "excluded":"UPDATED-EXCLUDED-DUBAI-FRAME"}
                        ]}
                        """
                            .formatted(quoteNumber)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("Rathin-Quote-")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String text = new String(pdf, StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(text)
        .contains("UPDATED-DESC-DUBAI-FRAME")
        .contains("UPDATED-TERMS-DUBAI-FRAME")
        .contains("UPDATED-INCLUDED-DUBAI-FRAME")
        .contains("UPDATED-EXCLUDED-DUBAI-FRAME")
        .contains("70.00")
        .doesNotContain("MASTER-DESC-SHOULD-NOT-WIN");
  }

  private MockHttpSession adminSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(apiLogin("Admin@rathin.com", "R@t@2023").session(session))
        .andExpect(status().isOk());
    return session;
  }

  private void upsertMaster(
      String itemId, String name, String description, String included, String excluded, String terms) {
    ItemMaster item = items.findByItemNameIgnoreCase(name).orElseGet(ItemMaster::new);
    Instant now = Instant.now();
    if (item.getItemId() == null) {
      item.setItemId(itemId);
      item.setCreatedDate(now);
      item.setCreatedBy("test");
    }
    item.setItemName(name);
    item.setDatedType(ItemMaster.DATED_TYPE_DA);
    item.setCategory("Fantasy");
    item.setBookingInfo(ItemMaster.BOOKING_INFO_DFT);
    item.setDescription(description);
    item.setIncluded(included);
    item.setExcluded(excluded);
    item.setTermsAndCondition(terms);
    item.setUpdatedDate(now);
    item.setUpdatedBy("test");
    items.save(item);
  }

  private static boolean tableExists(JdbcTemplate jdbc, String table) {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE lower(table_name) = ?
            """,
            Integer.class,
            table.toLowerCase(Locale.ROOT));
    return count != null && count > 0;
  }
}

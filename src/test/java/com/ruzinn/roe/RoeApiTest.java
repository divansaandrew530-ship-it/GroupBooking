package com.ruzinn.roe;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruzinn.service.RoeService;
import java.util.List;
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
class RoeApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DataSource dataSource;

  @Test
  void seedsUsdAndInrMasterAndConvertsQuotePdfEmailAndBooking() throws Exception {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "roe_master"))
        .contains("currency", "rate", "updated_date", "updated_by");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList("select currency from roe_master order by currency", String.class))
        .isEqualTo(List.of("INR", "USD"));

    mockMvc
        .perform(post("/api/admin/roe").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());

    MockHttpSession admin = login("Admin@rathin.com", "R@t@2023");
    mockMvc
        .perform(get("/api/parks/roe").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.base").value("AED"))
        .andExpect(jsonPath("$.rates[0].currency").value("AED"))
        .andExpect(jsonPath("$.rates[0].rate").value(1.0))
        .andExpect(jsonPath("$.rates[*].currency", Matchers.hasItems("USD", "INR")))
        .andExpect(jsonPath("$.rates[*].currency", Matchers.not(Matchers.hasItem("EUR"))));

    mockMvc
        .perform(
            post("/api/admin/roe")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usdRate\":0.5,\"inrRate\":20}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rates[?(@.currency == 'USD')].rate", Matchers.hasItem(0.5)));

    String aedPayload =
        """
        {"adults":1,"children":0,"markupPercent":0,"email":"roe.aed@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
        """;
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(aedPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("AED"))
        .andExpect(jsonPath("$.total").value(45.0))
        .andExpect(jsonPath("$.aedTotal").value(45.0))
        .andExpect(jsonPath("$.convertedValue").value(45.0));

    String usdPayload =
        """
        {"adults":1,"children":0,"markupPercent":0,"currency":"USD","email":"roe.usd@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
        """;
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(usdPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.roeRate").value(0.5))
            .andExpect(jsonPath("$.aedTotal").value(45.0))
            .andExpect(jsonPath("$.total").value(22.5))
            .andExpect(jsonPath("$.convertedValue").value(22.5))
            .andExpect(jsonPath("$.items[0].currency").value("USD"))
            .andExpect(jsonPath("$.items[0].lineTotal").value(22.5))
            .andExpect(jsonPath("$.items[0].aedLineTotal").value(45.0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");

    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select quote_value from quote_main where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(45.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select currency from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("USD");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select converted_value from quote_main where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(22.5);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select line_total from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .doubleValue())
        .isEqualTo(45.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select converted_rate from quote_details where quote_ref_num = ?",
                Number.class,
                quoteNumber)
                .doubleValue())
        .isEqualTo(22.5);

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.convertedValue").value(22.5))
        .andExpect(jsonPath("$.roeRate").value(0.5));

    mockMvc
        .perform(
            post("/api/parks/quote/pdf")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(usdPayload))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));

    mockMvc
        .perform(
            post("/api/parks/quote/send")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(usdPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sent").value(true));

    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"currency":"EUR","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Quote currency must be AED, USD, or INR"));

    String booked =
        mockMvc
            .perform(
                post("/api/parks/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"currency":"USD","leadName":"Roe Guest","email":"roe.book@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value(45.0))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.convertedValue").value(22.5))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.details[0].status").value("PENDING"))
            .andExpect(jsonPath("$.details[0].totalRate").value(45.0))
            .andExpect(jsonPath("$.details[0].convertedRate").value(22.5))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String bookingRef = com.jayway.jsonpath.JsonPath.read(booked, "$.bookingRefNum");
    mockMvc
        .perform(get("/api/parks/bookings/" + bookingRef).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.convertedValue").value(22.5))
        .andExpect(jsonPath("$.roeRate").value(0.5))
        .andExpect(jsonPath("$.status").value("PENDING"));

    mockMvc
        .perform(
            post("/api/admin/roe")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"usdRate\":"
                        + RoeService.DEFAULT_USD.toPlainString()
                        + ",\"inrRate\":"
                        + RoeService.DEFAULT_INR.toPlainString()
                        + "}"))
        .andExpect(status().isOk());
  }

  @Test
  void companyUserCannotUpdateRoe() throws Exception {
    String email = "roe.child." + System.nanoTime() + "@example.test";
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Roe Child Co",
                          "userName": "Roe Child",
                          "email": "%s",
                          "phone": "+971500000777",
                          "password": "ChildPass12"
                        }
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    MockHttpSession company = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(company)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/roe")
                .session(company)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usdRate\":1,\"inrRate\":1}"))
        .andExpect(status().isUnauthorized());
  }

  private MockHttpSession login(String email, String password) throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(apiLogin(email, password).session(session))
        .andExpect(status().isOk());
    return session;
  }

  private static List<String> tableColumns(JdbcTemplate jdbc, String table) {
    return jdbc
        .queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE lower(table_name) = ?
            ORDER BY ordinal_position
            """,
            String.class,
            table.toLowerCase(Locale.ROOT))
        .stream()
        .map(name -> name.toLowerCase(Locale.ROOT))
        .toList();
  }
}

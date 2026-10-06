package com.ruzinn.parks;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class BookingPartyApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DataSource dataSource;

  @Test
  void parentBookingPartyListsChildCompaniesAndUsers() throws Exception {
    ChildCompany child = registerChild("Party Child Co", "party.child@example.com", "PartyChild1");
    mockMvc
        .perform(get("/api/parks/booking-party").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentCompany").value(true))
        .andExpect(jsonPath("$.companies[*].companyName", Matchers.hasItem("Party Child Co")))
        .andExpect(jsonPath("$.companies[?(@.companyName=='Party Child Co')].users[0].email")
            .value(Matchers.hasItem("party.child@example.com")))
        .andExpect(jsonPath("$.owners[*].email", Matchers.hasItem("admin@rathin.com")));
    mockMvc
        .perform(get("/api/parks/booking-party").session(child.session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentCompany").value(false))
        .andExpect(jsonPath("$.companyId").value(child.companyId))
        .andExpect(jsonPath("$.companyName").value("Party Child Co"))
        .andExpect(jsonPath("$.contactEmail").value("party.child@example.com"))
        .andExpect(jsonPath("$.companies").isEmpty());
  }

  @Test
  void bookingPartyDoesNotRequireSignIn() throws Exception {
    registerChild("Anon Party Co", "anon.party@example.com", "AnonParty1");
    mockMvc
        .perform(get("/api/parks/booking-party"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentCompany").value(true))
        .andExpect(jsonPath("$.companies[*].companyName", Matchers.hasItem("Anon Party Co")));
  }

  @Test
  void parentQuoteStoresChildCompanyContactAndOwner() throws Exception {
    ChildCompany child = registerChild("Quote Party Co", "quote.party@example.com", "QuoteParty1");
    String me =
        mockMvc
            .perform(get("/api/auth/me").session(adminSession()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String ownerUserId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(me, "$.userId"));
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "contactUserId":"%s",
                          "ownerUserId":"%s",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(child.companyId, child.userId, ownerUserId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Rathin"))
            .andExpect(jsonPath("$.preparedBy").value("Rathin Super Admin"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("1");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Quote Party Co Admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_email FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("quote.party@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin Super Admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("admin@rathin.com");
    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "quoteNumber":"%s",
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "contactUserId":"%s",
                          "ownerUserId":"%s",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(quoteNumber, child.companyId, child.userId, ownerUserId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    org.assertj.core.api.Assertions.assertThat(pdf).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value("1"))
        .andExpect(jsonPath("$.companyName").value("Rathin"))
        .andExpect(jsonPath("$.customerName").value("Quote Party Co Admin"));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(child.session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void parentEnterDetailsQuoteStoresWalkInCustomerOnParentCompany() throws Exception {
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "leadName":"Walk Customer",
                          "email":"walk.customer@example.com",
                          "phone":"+971500001111",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Rathin"))
            .andExpect(jsonPath("$.preparedBy").value("Rathin Super Admin"))
            .andExpect(jsonPath("$.preparedBy").value(Matchers.not("Walk Customer")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("1");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Walk Customer");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_email FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("walk.customer@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_phone FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("+971500001111");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin Super Admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin")
        .isNotEqualTo("Direct");
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("Rathin Super Admin"))
        .andExpect(jsonPath("$.customerName").value("Walk Customer"))
        .andExpect(jsonPath("$.customerEmail").value("walk.customer@example.com"))
        .andExpect(jsonPath("$.companyName").value(Matchers.not("Direct")));
  }

  @Test
  void customerDetailsAreVisibleOnlyToOwningCompanyUsers() throws Exception {
    ChildCompany child = registerChild("Owned Party Co", "owned.party@example.com", "OwnedParty1");
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "contactUserId":"%s",
                          "leadName":"Hidden Walk Guest",
                          "email":"hidden.walk@example.com",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(child.companyId, child.userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.preparedBy").value("Rathin Super Admin"))
            .andExpect(jsonPath("$.companyName").value("Rathin"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value("1"))
        .andExpect(jsonPath("$.customerName").value("Owned Party Co Admin"))
        .andExpect(jsonPath("$.customerEmail").value("owned.party@example.com"));
    mockMvc
        .perform(get("/api/company/quotes").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.hasItem(quoteNumber)));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(child.session))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/company/quotes").session(child.session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.not(Matchers.hasItem(quoteNumber))));
  }

  @Test
  void parentSelectCompanyStoresCreatingCompany() throws Exception {
    ChildCompany child = registerChild("Optional User Co", "optional.user@example.com", "OptionalUser1");
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(child.companyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Rathin"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("1");
  }

  @Test
  void parentCanDownloadAndEditLegacyBookingPartyQuoteStoredOnChildCompany() throws Exception {
    ChildCompany child = registerChild("Legacy Party Co", "legacy.party@example.com", "LegacyParty1");
    String me =
        mockMvc
            .perform(get("/api/auth/me").session(adminSession()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String ownerUserId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(me, "$.userId"));
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "contactUserId":"%s",
                          "ownerUserId":"%s",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(child.companyId, child.userId, ownerUserId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update(
        "UPDATE quote_main SET company_id = ?, company_name = ? WHERE quote_ref_num = ?",
        child.companyId,
        "Legacy Party Co",
        quoteNumber);
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "quoteNumber":"%s",
                      "adults":1,
                      "children":0,
                      "markupPercent":0,
                      "companyId":"%s",
                      "contactUserId":"%s",
                      "ownerUserId":"%s",
                      "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                    }
                    """
                        .formatted(quoteNumber, child.companyId, child.userId, ownerUserId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteNumber").value(quoteNumber));
    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "quoteNumber":"%s",
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"%s",
                          "contactUserId":"%s",
                          "ownerUserId":"%s",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """
                            .formatted(quoteNumber, child.companyId, child.userId, ownerUserId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    org.assertj.core.api.Assertions.assertThat(pdf).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(quoteNumber));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("1");
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(child.session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void parentRejectsInvalidPartySelections() throws Exception {
    ChildCompany child = registerChild("Reject Party Co", "reject.party@example.com", "RejectParty1");
    ChildCompany other = registerChild("Other Party Co", "other.party@example.com", "OtherParty1");
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"companyId":"WGT-B2B","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Select a child company"));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"companyId":"999991","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Company not found"));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adults":1,
                      "children":0,
                      "companyId":"%s",
                      "contactUserId":"%s",
                      "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                    }
                    """
                        .formatted(child.companyId, other.userId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Contact person must belong to the selected company"));
  }

  @Test
  void childQuoteIgnoresOtherCompanyAndUsesSignedInContact() throws Exception {
    ChildCompany child = registerChild("Forced Party Co", "forced.party@example.com", "ForcedParty1");
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(child.session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":1,
                          "children":0,
                          "markupPercent":0,
                          "companyId":"1",
                          "leadName":"Someone Else",
                          "email":"other.guest@example.com",
                          "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Forced Party Co"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo(child.companyId);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_email FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("forced.party@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Forced Party Co Admin");
  }

  @Test
  void parentProcessBookingKeepsSelectedChildCompany() throws Exception {
    ChildCompany child = registerChild("Book Party Co", "book.party@example.com", "BookParty1");
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"companyId":"%s",
                     "leadName":"Walk Customer","email":"walk.customer@example.com","phone":"+971500009999",
                     "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}],
                     "paymentMethod":"CARD","cardName":"Walk Customer","cardNumber":"4242424242424242",
                     "cardExpiry":"09/28","cardCvc":"123"}
                    """
                        .formatted(child.companyId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadName").value("Walk Customer"))
        .andExpect(jsonPath("$.email").value("walk.customer@example.com"));
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM booking_main WHERE user_name = ?", String.class, "Walk Customer"))
        .isEqualTo(child.companyId);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM booking_main WHERE user_name = ?", String.class, "Walk Customer"))
        .isEqualTo("Book Party Co");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_id FROM booking_main WHERE user_name = ?", String.class, "Walk Customer"))
        .isEqualTo("admin@rathin.com");
  }

  @Test
  void parentCannotBookForUnapprovedCompany() throws Exception {
    String pendingId = registerPendingChild("Pending Party Co", "pending.party@example.com", "PendParty1");
    MockHttpSession admin = adminSession();
    mockMvc
        .perform(get("/api/parks/booking-party").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companies[*].companyName", Matchers.not(Matchers.hasItem("Pending Party Co"))));
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"companyId":"%s",
                     "leadName":"Pending Guest","email":"pending.guest@example.com","phone":"+971500009998",
                     "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}],
                     "paymentMethod":"CARD","cardName":"Pending Guest","cardNumber":"4242424242424242",
                     "cardExpiry":"09/28","cardCvc":"123"}
                    """
                        .formatted(pendingId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Pending Party Co is not approved yet"));
    Integer bookings =
        new JdbcTemplate(dataSource)
            .queryForObject("SELECT COUNT(*) FROM booking_main WHERE user_name = ?", Integer.class, "Pending Guest");
    org.assertj.core.api.Assertions.assertThat(bookings).isZero();
  }

  @Test
  void parentCreditBookingChargesSelectedChildCompany() throws Exception {
    ChildCompany child = registerChild("Credit Party Co", "credit.party@example.com", "CreditParty1");
    MockHttpSession admin = adminSession();
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    java.math.BigDecimal parentUsedBefore =
        jdbc.queryForObject("SELECT credit_used FROM company WHERE company_id = 1", java.math.BigDecimal.class);
    mockMvc
        .perform(get("/api/company/credit").param("companyId", child.companyId).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remaining").value(500.0))
        .andExpect(jsonPath("$.canUseCredit").value(true));
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"companyId":"%s",
                     "leadName":"Credit Guest","email":"credit.guest@example.com","phone":"+971500009997",
                     "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}],
                     "paymentMethod":"CREDIT"}
                    """
                        .formatted(child.companyId)))
        .andExpect(status().isOk());
    java.math.BigDecimal childUsed =
        jdbc.queryForObject(
            "SELECT credit_used FROM company WHERE company_id = ?", java.math.BigDecimal.class, Long.valueOf(child.companyId));
    org.assertj.core.api.Assertions.assertThat(childUsed).isPositive();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT credit_used FROM company WHERE company_id = 1", java.math.BigDecimal.class))
        .isEqualByComparingTo(parentUsedBefore);
  }

  @Test
  void childCannotReadAnotherCompanysCredit() throws Exception {
    ChildCompany child = registerChild("Nosy Party Co", "nosy.party@example.com", "NosyParty1");
    mockMvc
        .perform(get("/api/company/credit").param("companyId", "1").session(child.session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void quoteWorkspaceReturnsQuoteAndViewerPermissions() throws Exception {
    MockHttpSession admin = adminSession();
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,
                         "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber + "/workspace").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote.quoteRefNum").value(quoteNumber))
        .andExpect(jsonPath("$.showActualRates").value(true))
        .andExpect(jsonPath("$.canManageQuote").value(true))
        .andExpect(jsonPath("$.statusOptions", Matchers.hasItem("ACCEPTED")));

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber + "/workspace"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void xlinePassengerDetailsAreSavedWithTheBooking() throws Exception {
    // Same shape the Angular checkout sends: field keys at the top level plus a "values" copy.
    String body =
        """
        {"adults":1,"children":0,"markupPercent":0,
         "leadName":"Pax Lead","email":"pax.lead@example.com","phone":"+971500001111",
         "items":[{"itemId":"xline-dubai-marina","visitDate":"2026-10-05","adults":1,"children":0,
           "passengers":[{"type":"ADULT","label":"Adult 1",
             "firstName":"Ravi","lastName":"Kumar","passport":"P1234567","dateOfBirth":"1990-05-17","age":"36",
             "values":{"firstName":"Ravi","lastName":"Kumar","passport":"P1234567","dateOfBirth":"1990-05-17","age":"36"}}]}],
         "paymentMethod":"CARD","cardName":"Pax Lead","cardNumber":"4242424242424242","cardExpiry":"09/28","cardCvc":"123"}
        """;
    String response =
        mockMvc
            .perform(post("/api/parks/confirm").session(adminSession()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String bookingRef = com.jayway.jsonpath.JsonPath.read(response, "$.bookingRef");

    String saved =
        new JdbcTemplate(dataSource)
            .queryForObject(
                "SELECT passengers FROM booking_details WHERE booking_ref_num = ?", String.class, bookingRef);
    org.assertj.core.api.Assertions.assertThat(saved)
        .contains("Ravi", "Kumar", "P1234567", "1990-05-17", "36");

    mockMvc
        .perform(get("/api/company/bookings/" + bookingRef).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.details[0].passengers[0].firstName").value("Ravi"))
        .andExpect(jsonPath("$.details[0].passengers[0].passport").value("P1234567"))
        .andExpect(jsonPath("$.details[0].passengers[0].dateOfBirth").value("1990-05-17"))
        .andExpect(jsonPath("$.details[0].passengers[0].age").value("36"));
  }

  @Test
  void dashboardReturnsKpisForSignedInUser() throws Exception {
    mockMvc.perform(get("/api/company/dashboard")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/company/dashboard").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dashboard.bookingsLast30Days").isNumber())
        .andExpect(jsonPath("$.dashboard.pendingParks").isNumber())
        .andExpect(jsonPath("$.dashboard.ticketsIssued").isNumber())
        .andExpect(jsonPath("$.dashboard.pendingTickets").isNumber());
  }

  @Test
  void bookingPartySelectionIsOfferedToParentOnly() throws Exception {
    ChildCompany child = registerChild("Ui Party Co", "ui.party@example.com", "UiParty1");
    mockMvc
        .perform(get("/api/parks/booking-party").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentCompany").value(true))
        .andExpect(jsonPath("$.companies[*].companyName", Matchers.hasItem("Ui Party Co")));
    mockMvc
        .perform(get("/api/parks/booking-party").session(child.session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentCompany").value(false));
  }

  private MockHttpSession adminSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(session))
        .andExpect(status().isOk());
    return session;
  }

  /** Registers and email-verifies a company but leaves it PENDING (no Super Admin approval). */
  private String registerPendingChild(String companyName, String email, String password) throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"companyName":"%s","userName":"%s Admin","email":"%s","phone":"+971500000887","password":"%s"}
                        """
                            .formatted(companyName, companyName, email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(new MockHttpSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(email, otp)))
        .andExpect(status().isOk());
    return String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));
  }

  private ChildCompany registerChild(String companyName, String email, String password) throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "%s",
                          "userName": "%s Admin",
                          "email": "%s",
                          "phone": "+971500000888",
                          "password": "%s"
                        }
                        """
                            .formatted(companyName, companyName, email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(email, otp)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"party child\",\"creditLimit\":\"500\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
        .andExpect(status().isOk());
    String me =
        mockMvc
            .perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(me, "$.userId"));
    return new ChildCompany(companyId, userId, session);
  }

  private record ChildCompany(String companyId, String userId, MockHttpSession session) {}
}

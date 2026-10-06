package com.ruzinn.parks;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruzinn.dto.SentBookingMail;
import com.ruzinn.service.BookingConfirmationMailService;
import com.ruzinn.service.ParkQuoteMailService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ParkQuoteApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DataSource dataSource;
  @Autowired private ParkQuoteMailService parkQuoteMailService;
  @Autowired private BookingConfirmationMailService bookingConfirmationMailService;

  private MockHttpSession adminSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(session))
        .andExpect(status().isOk());
    return session;
  }

  @Test
  void bookingTablesMatchMainAndDetailsSchema() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "booking_main"))
        .containsExactlyInAnyOrder(
            "booking_ref_num",
            "user_name",
            "user_id",
            "company_name",
            "company_id",
            "booking_value",
            "currency",
            "roe_rate",
            "converted_value",
            "booking_status",
            "lead_email",
            "lead_phone",
            "voucher_file_name",
            "voucher_content_type",
            "voucher_bytes",
            "ticket_file_name",
            "ticket_content_type",
            "ticket_bytes",
            "created_date");
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "booking_details"))
        .contains(
            "id",
            "booking_ref_num",
            "service_name",
            "service_id",
            "item_name",
            "show_date",
            "checkin_date",
            "checkout_date",
            "property_id",
            "adult_count",
            "child_count",
            "infant_count",
            "toddler_count",
            "total_rate",
            "roe_rate",
            "converted_rate",
            "booked_on",
            "booked_by",
            "user_id",
            "company_name",
            "company_id",
            "inventory_type",
            "supplier_name",
            "supplier_id",
            "item_status",
            "follow_up",
            "voucher_file_name",
            "voucher_content_type",
            "voucher_bytes",
            "ticket_file_name",
            "ticket_content_type",
            "ticket_bytes",
            "file_path");
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "quote_main"))
        .contains(
            "quote_ref_num",
            "user_name",
            "user_id",
            "company_name",
            "company_id",
            "quote_value",
            "currency",
            "roe_rate",
            "converted_value",
            "quote_status",
            "customer_name",
            "customer_email",
            "customer_phone",
            "follow_up",
            "sent_to",
            "sent_at",
            "markup_percent",
            "subtotal",
            "markup_amount",
            "created_date",
            "expire_date",
            "updated_date",
            "booking_ref_num");
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "quote_details"))
        .contains(
            "id",
            "quote_ref_num",
            "item_id",
            "item_name",
            "kind",
            "category",
            "visit_date",
            "checkout_date",
            "adult_count",
            "child_count",
            "rooms",
            "room_type",
            "time_slot",
            "line_total",
            "roe_rate",
            "converted_rate",
            "adult_rate",
            "child_rate",
            "description",
            "terms_and_condition",
            "included",
            "excluded",
            "created_date");
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "change_history"))
        .contains(
            "id",
            "entity_type",
            "entity_id",
            "booking_ref_num",
            "action",
            "field_name",
            "old_value",
            "new_value",
            "actor_user_id",
            "actor_user_name",
            "actor_email",
            "actor_role",
            "company_id",
            "company_name",
            "changed_at");
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

  @Test
  void catalogIncludesSeptemberPriceList() throws Exception {
    mockMvc
        .perform(get("/api/parks/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("AED"))
        // 90 price-list experiences, plus any items other tests added through Item Master.
        .andExpect(jsonPath("$.experiences.length()").value(Matchers.greaterThanOrEqualTo(90)))
        .andExpect(jsonPath("$.extras.length()").value(7))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'ferrari-world')].adultPrice")
                .value(Matchers.hasItem(280.0)))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'ferrari-world')].datedType")
                .value(Matchers.hasItem("DA")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'museum-of-the-future')].datedType")
                .value(Matchers.hasItem("DT")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'museum-of-the-future')].apiTimeSlot")
                .value(Matchers.hasItem(true)))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'sky-views-glass-slide-combo')].apiTimeSlot")
                .value(Matchers.hasItem(false)))
        .andExpect(
            jsonPath("$.extras[?(@.id == 'sea-view-hotel-bur-dubai')].starCategory")
                .value(Matchers.hasItem(4)))
        .andExpect(
            jsonPath("$.extras[?(@.id == 'sea-view-hotel-bur-dubai')].roomRates[*].name")
                .value(Matchers.hasItem("Standard")))
        .andExpect(
            jsonPath("$.extras[?(@.id == 'sea-view-hotel-bur-dubai')].roomRates[*].price")
                .value(Matchers.hasItem(150.0)))
        .andExpect(
            jsonPath("$.extras[?(@.id == '30-days-visa')].adultPrice")
                .value(Matchers.hasItem(300.0)))
        .andExpect(
            jsonPath("$.extras[?(@.id == '60-days-visa')].adultPrice")
                .value(Matchers.hasItem(640.0)))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'ferrari-world')].bookingInfo")
                .value(Matchers.hasItem("DFT")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'ferrari-world')].passengerFields")
                .value(Matchers.hasItem(Matchers.contains("firstName", "lastName", "phone", "email"))))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'xline-dubai-marina')].bookingInfo")
                .value(Matchers.hasItem("ADD_ON")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'xline-dubai-marina')].passengerFields")
                .value(Matchers.hasItem(Matchers.hasItems("firstName", "lastName", "passport", "dateOfBirth", "age"))))
        .andExpect(jsonPath("$.countries.length()").value(3))
        .andExpect(jsonPath("$.countries[0].code").value("UAE"))
        .andExpect(jsonPath("$.countries[0].flag").value("ae"))
        .andExpect(jsonPath("$.countries[1].code").value("IND"))
        .andExpect(jsonPath("$.countries[1].flag").value("in"))
        .andExpect(jsonPath("$.countries[2].code").value("SIN"))
        .andExpect(jsonPath("$.countries[2].flag").value("sg"))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'dubai-frame')].country").value(Matchers.hasItem("UAE")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'ferrari-world')].country").value(Matchers.hasItem("IND")))
        .andExpect(
            jsonPath("$.experiences[?(@.id == 'transfer-half-day')].country")
                .value(Matchers.hasItem("SIN")))
        .andExpect(
            jsonPath("$.extras[?(@.id == 'sea-view-hotel-bur-dubai')].country")
                .value(Matchers.hasItem("UAE")));
  }

  @Test
  void directCustomerQuoteSavesWalkInHotelAndDayAttractionsForEditQuote() throws Exception {
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "currency":"AED",
                          "leadName":"Aisha Direct",
                          "email":"aisha.direct@example.com",
                          "phone":"+971500002222",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-11-11","checkOut":"2026-11-17","adults":2,"children":0,"rooms":1,"roomType":"Standard"},
                            {"itemId":"dubai-frame","visitDate":"2026-11-12","adults":2,"children":0},
                            {"itemId":"national-aquarium-abu-dhabi-standard","visitDate":"2026-11-16","adults":2,"children":0}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteNumber").exists())
            .andExpect(jsonPath("$.companyName").value("Rathin"))
            .andExpect(jsonPath("$.preparedBy").value(Matchers.not("Aisha Direct")))
            .andExpect(jsonPath("$.items.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Aisha Direct");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin")
        .isNotEqualTo("Direct");
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(quoteNumber))
        .andExpect(jsonPath("$.customerName").value("Aisha Direct"))
        .andExpect(jsonPath("$.details.length()").value(3));
  }

  @Test
  void quotesListLoadsWhenDirectCustomerColumnIsNull() throws Exception {
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"email":"null.direct@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update("update quote_main set direct_customer = null where quote_ref_num = ?", quoteNumber);
    mockMvc
        .perform(get("/api/company/quotes").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.hasItem(quoteNumber)));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(quoteNumber));
  }

  @Test
  void directCustomerQuoteStoresPackageCopyAndRendersHolidayPdf() throws Exception {
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "currency":"AED",
                          "leadName":"Aisha Direct",
                          "email":"aisha.direct@example.com",
                          "phone":"+971500002222",
                          "packageInclusions":"PACKAGE-INCLUSIONS-DIRECT-TEST",
                          "packageExclusions":"PACKAGE-EXCLUSIONS-DIRECT-TEST",
                          "guestInformation":"GUEST-INFO-DIRECT-TEST",
                          "dayPlans":[
                            {"date":"2026-11-11","title":"Arrival in Dubai","note":"DAY-NOTE-ARRIVAL-TEST"},
                            {"date":"2026-11-12","title":"Dubai Frame","note":"DAY-NOTE-FRAME-TEST"}
                          ],
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-11-11","checkOut":"2026-11-17","adults":2,"children":0,"rooms":1,"roomType":"Standard"},
                            {"itemId":"dubai-frame","visitDate":"2026-11-12","adults":2,"children":0}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.directCustomer").value(true))
            .andExpect(jsonPath("$.customerName").value("Aisha Direct"))
            .andExpect(jsonPath("$.customerEmail").value("aisha.direct@example.com"))
            .andExpect(jsonPath("$.packageTitle").value("UAE Holiday Package"))
            .andExpect(jsonPath("$.packageInclusions").value("PACKAGE-INCLUSIONS-DIRECT-TEST"))
            .andExpect(jsonPath("$.packageExclusions").value("PACKAGE-EXCLUSIONS-DIRECT-TEST"))
            .andExpect(jsonPath("$.guestInformation").value("GUEST-INFO-DIRECT-TEST"))
            .andExpect(jsonPath("$.dayPlans[0].note").value("DAY-NOTE-ARRIVAL-TEST"))
            .andExpect(jsonPath("$.durationLabel").value("6N / 7D"))
            .andExpect(jsonPath("$.durationPhrase").value("6 nights / 7 days"))
            .andExpect(jsonPath("$.stayDetailLine").value("Standard room | 6 nights | 1 room"))
            .andExpect(jsonPath("$.hotelName").value("Sea View Hotel - Bur Dubai"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT package_inclusions FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("PACKAGE-INCLUSIONS-DIRECT-TEST");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT COALESCE(direct_customer, false) FROM quote_main WHERE quote_ref_num = ?", Boolean.class, quoteNumber))
        .isTrue();
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.directCustomer").value(true))
        .andExpect(jsonPath("$.customerName").value("Aisha Direct"))
        .andExpect(jsonPath("$.packageInclusions").value("PACKAGE-INCLUSIONS-DIRECT-TEST"))
        .andExpect(jsonPath("$.dayPlans[0].title").value("Arrival in Dubai"));

    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Aisha Direct",
                          "email":"aisha.direct@example.com",
                          "phone":"+971500002222",
                          "packageInclusions":"PACKAGE-INCLUSIONS-DIRECT-TEST",
                          "packageExclusions":"PACKAGE-EXCLUSIONS-DIRECT-TEST",
                          "guestInformation":"GUEST-INFO-DIRECT-TEST",
                          "dayPlans":[
                            {"date":"2026-11-11","title":"Arrival in Dubai","note":"DAY-NOTE-ARRIVAL-TEST"}
                          ],
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-11-11","checkOut":"2026-11-17","adults":2,"children":0,"rooms":1,"roomType":"Standard"}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String text = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(text)
        .contains("Holiday Package")
        .contains("Day-by-Day Itinerary")
        .contains("6 nights / 7 days")
        .contains("Total package amount")
        .contains("Inclusive of listed arrangements")
        .contains("CUSTOMER")
        .contains("GUESTS")
        .contains("Private holiday package")
        .contains("ACCOMMODATION")
        .contains("PACKAGE PRICE")
        .contains("PREPARED BY")
        .contains("Standard room")
        .contains("Package Inclusions")
        .contains("Package Exclusions")
        .contains("Important Guest Information")
        .contains("PACKAGE-INCLUSIONS-DIRECT-TEST")
        .contains("PACKAGE-EXCLUSIONS-DIRECT-TEST")
        .contains("GUEST-INFO-DIRECT-TEST")
        .contains("DAY-NOTE-ARRIVAL-TEST")
        .contains("Arrival in Dubai")
        .contains("Day 1")
        .contains("Aisha Direct")
        .contains("aisha.direct@example.com")
        .contains("Page ")
        .doesNotContain("Quoted itinerary")
        .doesNotContain("PARK DETAILS")
        .doesNotContain("6N / 7D");
  }

  @Test
  void directQuoteKeepsTypedCustomerAndOpensDirectEditor() throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Direct Child Co",
                          "userName": "Direct Child Admin",
                          "email": "direct.child@example.com",
                          "phone": "+971500000778",
                          "password": "DirectChild1"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));
    MockHttpSession childSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"direct.child@example.com\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"direct child\",\"creditLimit\":\"500\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"direct.child@example.com\",\"password\":\"DirectChild1\"}"))
        .andExpect(status().isOk());

    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Walk Guest",
                          "email":"walk.guest@example.com",
                          "phone":"+971500009111",
                          "packageInclusions":"Breakfast",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":0,"rooms":1,"roomType":"Standard"}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.directCustomer").value(true))
            .andExpect(jsonPath("$.customerName").value("Walk Guest"))
            .andExpect(jsonPath("$.customerEmail").value("walk.guest@example.com"))
            .andExpect(jsonPath("$.preparedBy").value("Direct Child Admin"))
            .andExpect(jsonPath("$.companyName").value("Direct Child Co"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Walk Guest");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT customer_email FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("walk.guest@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_name FROM quote_main WHERE quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Direct Child Admin");

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerName").value("Walk Guest"))
        .andExpect(jsonPath("$.directCustomer").value(true));
    mockMvc
        .perform(get("/api/auth/me").session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyType").value(Matchers.not("PARENT")));
    mockMvc
        .perform(get("/api/auth/me").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
  }

  @Test
  void childCompanyCanOverrideDirectLineFaresAndParentCannot() throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Fare Child Co",
                          "userName": "Fare Child Admin",
                          "email": "fare.child@example.com",
                          "phone": "+971500000779",
                          "password": "FareChild1"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));
    MockHttpSession childSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fare.child@example.com\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"fare child\",\"creditLimit\":\"500\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fare.child@example.com\",\"password\":\"FareChild1\"}"))
        .andExpect(status().isOk());

    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":1,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Fare Guest",
                          "email":"fare.guest@example.com",
                          "phone":"+971500009222",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":1,"rooms":1,"roomType":"Standard","adultRate":200},
                            {"itemId":"dubai-frame","visitDate":"2026-10-08","adults":2,"children":1,"adultRate":80,"childRate":40}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.directCustomer").value(true))
            .andExpect(jsonPath("$.items[0].adultPrice").value(200.0))
            .andExpect(jsonPath("$.items[0].lineTotal").value(400.0))
            .andExpect(jsonPath("$.items[0].actualAdultPrice").value(150.0))
            .andExpect(jsonPath("$.items[0].actualLineTotal").value(300.0))
            .andExpect(jsonPath("$.items[1].adultPrice").value(80.0))
            .andExpect(jsonPath("$.items[1].childPrice").value(40.0))
            .andExpect(jsonPath("$.items[1].lineTotal").value(200.0))
            .andExpect(jsonPath("$.items[1].actualAdultPrice").value(45.0))
            .andExpect(jsonPath("$.items[1].actualChildPrice").value(25.0))
            .andExpect(jsonPath("$.items[1].actualLineTotal").value(115.0))
            .andExpect(jsonPath("$.total").value(600.0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select adult_rate from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(200.0);

    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "directCustomer":true,
                      "adults":2,
                      "children":0,
                      "markupPercent":0,
                      "country":"UAE",
                      "leadName":"Parent Guest",
                      "email":"parent.fare@example.com",
                      "items":[
                        {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":0,"rooms":1,"roomType":"Standard","adultRate":999},
                        {"itemId":"dubai-frame","visitDate":"2026-10-08","adults":2,"children":0,"adultRate":999,"childRate":999}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].adultPrice").value(150.0))
        .andExpect(jsonPath("$.items[0].lineTotal").value(300.0))
        .andExpect(jsonPath("$.items[1].adultPrice").value(45.0))
        .andExpect(jsonPath("$.items[1].childPrice").value(25.0))
        .andExpect(jsonPath("$.items[1].lineTotal").value(90.0));

    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "directCustomer":true,
                      "quoteNumber":"%s",
                      "adults":2,
                      "children":1,
                      "markupPercent":0,
                      "country":"UAE",
                      "leadName":"Fare Guest",
                      "email":"fare.guest@example.com",
                      "items":[
                        {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":1,"rooms":1,"roomType":"Standard","adultRate":1},
                        {"itemId":"dubai-frame","visitDate":"2026-10-08","adults":2,"children":1,"adultRate":1,"childRate":1}
                      ]
                    }
                    """
                        .formatted(quoteNumber)))
        .andExpect(status().isUnauthorized());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select adult_rate from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(200.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select adult_rate from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "dubai-frame")
                .doubleValue())
        .isEqualTo(80.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_adult_rate from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(150.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_line_total from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(300.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_adult_rate from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "dubai-frame")
                .doubleValue())
        .isEqualTo(45.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_line_total from quote_details where quote_ref_num = ? and item_id = ?",
                Number.class,
                quoteNumber,
                "dubai-frame")
                .doubleValue())
        .isEqualTo(115.0);

    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "quoteNumber":"%s",
                          "adults":2,
                          "children":1,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Fare Guest",
                          "email":"fare.guest@example.com",
                          "phone":"+971500009222",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":1,"rooms":1,"roomType":"Standard","adultRate":200},
                            {"itemId":"dubai-frame","visitDate":"2026-10-08","adults":2,"children":1,"adultRate":80,"childRate":40}
                          ]
                        }
                        """
                            .formatted(quoteNumber)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String pdfText = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(pdfText).contains("600.00").doesNotContain("415.00");

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(600.0))
        .andExpect(jsonPath("$.details[*].lineTotal", Matchers.hasItems(400.0, 200.0)))
        .andExpect(jsonPath("$.details[*].actualLineTotal", Matchers.hasItems(300.0, 115.0)));

    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "directCustomer":true,
                      "quoteNumber":"%s",
                      "acceptQuote":true,
                      "adults":2,
                      "children":1,
                      "markupPercent":0,
                      "country":"UAE",
                      "leadName":"Fare Guest",
                      "email":"fare.guest@example.com",
                      "phone":"+971500009222",
                      "items":[
                        {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":1,"rooms":1,"roomType":"Standard","adultRate":200},
                        {"itemId":"dubai-frame","visitDate":"2026-10-08","adults":2,"children":1,"adultRate":80,"childRate":40}
                      ],
                      "paymentMethod":"CARD","cardName":"Fare Guest","cardNumber":"4242424242424242",
                      "cardExpiry":"09/28","cardCvc":"123"
                    }
                    """
                        .formatted(quoteNumber)))
        .andExpect(status().isOk());
    String bookingRef =
        jdbc.queryForObject(
            "select booking_ref_num from quote_main where quote_ref_num = ?", String.class, quoteNumber);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select actual_total_rate from booking_details where booking_ref_num = ? and service_id = ?",
                Number.class,
                bookingRef,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(300.0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select total_rate from booking_details where booking_ref_num = ? and service_id = ?",
                Number.class,
                bookingRef,
                "sea-view-hotel-bur-dubai")
                .doubleValue())
        .isEqualTo(400.0);

    mockMvc
        .perform(get("/api/company/bookings/" + bookingRef).session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(600.0))
        .andExpect(jsonPath("$.showQuotedRate").value(true))
        .andExpect(jsonPath("$.hideQuotedRate").value(false));
    mockMvc
        .perform(get("/api/company/bookings/" + bookingRef).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(415.0))
        .andExpect(jsonPath("$.hideQuotedRate").value(true))
        .andExpect(jsonPath("$.showQuotedRate").value(false));
  }

  @Test
  void childCompanyLogoBrandsDirectQuotePdf() throws Exception {
    MockHttpSession registerSession = new MockHttpSession();
    MockMultipartFile logo =
        new MockMultipartFile("logo", "logo-child.jpg", MediaType.IMAGE_JPEG_VALUE, jpegLogo("LOGOCHILD"));
    String registered =
            mockMvc
                .perform(
                    multipart("/api/auth/register")
                        .file(logo)
                        .param("companyName", "Logo Child Co")
                        .param("userName", "Logo Child Admin")
                        .param("email", "logo.child@example.com")
                        .param("phone", "+971500000880")
                        .param("password", "LogoChild1")
                        .param("registrationNo", "LC-880")
                        .param("address", "Marina Plaza")
                        .param("website", "https://logochild.example")
                        .param("country", "United Arab Emirates")
                        .param("city", "Dubai")
                        .param("zipCode", "00000")
                        .param("ownerTelephone", "+97143000088")
                        .param("operationsStaffName", "Ops Logo")
                        .param("operationsStaffNumber", "+971500000881")
                        .param("financeName", "Finance Logo")
                        .param("financeMobile", "+971500000882")
                        .param("financeTelephone", "+97143000089")
                        .param("financeEmail", "accounts@logochild.example"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(registerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"logo.child@example.com\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    String companyId =
        String.valueOf(
            jdbc.queryForObject(
                "select company_id from company where lower(email) = ?", Object.class, "logo.child@example.com"));
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"logo child\",\"creditLimit\":\"500\"}"))
        .andExpect(status().isOk());
    MockHttpSession childSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"logo.child@example.com\",\"password\":\"LogoChild1\"}"))
        .andExpect(status().isOk());

    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Logo Guest",
                          "email":"logo.guest@example.com",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":0,"rooms":1,"roomType":"Standard"}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Logo Child Co"))
            .andExpect(jsonPath("$.brandName").value("Logo Child Co"))
            .andExpect(jsonPath("$.brandAddress").value("Marina Plaza"))
            .andExpect(jsonPath("$.brandEmail").value("logo.child@example.com"))
            .andExpect(jsonPath("$.brandPhone").value("+971500000880"))
            .andExpect(jsonPath("$.hasLogo").value(true))
            .andExpect(jsonPath("$.logoUrl").value("/companies/" + companyId + "/logo"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(quoted).contains("Logo Child Co");

    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "directCustomer":true,
                          "adults":2,
                          "children":0,
                          "markupPercent":0,
                          "country":"UAE",
                          "leadName":"Logo Guest",
                          "email":"logo.guest@example.com",
                          "items":[
                            {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-07","checkOut":"2026-10-09","adults":2,"children":0,"rooms":1,"roomType":"Standard"}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String text = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    org.assertj.core.api.Assertions.assertThat(text)
        .contains("Logo Child Co")
        .contains("Marina Plaza")
        .contains("logo.child@example.com")
        .contains("+971500000880");
    mockMvc
        .perform(get("/companies/" + companyId + "/logo").session(childSession))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
  }

  @Test
  void quotePopupSendAndPdfDoNotCreateBooking() throws Exception {
    String payload =
        """
        {"adults":1,"children":0,"markupPercent":0,"email":"quote.guest@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
        """;
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Number before = jdbc.queryForObject("select count(*) from booking_main", Number.class);
    parkQuoteMailService.clearSent();
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteNumber").value(Matchers.startsWith("QTE")))
            .andExpect(jsonPath("$.items[0].name").value("Dubai Frame"))
            .andExpect(jsonPath("$.total").value(45.0))
            .andExpect(jsonPath("$.preparedBy").value("Rathin Super Admin"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    String trackedPayload =
        """
        {"adults":1,"children":0,"markupPercent":0,"email":"quote.guest@example.com","quoteNumber":"%s","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
        """
            .formatted(quoteNumber);
    mockMvc
        .perform(
            post("/api/parks/quote/send")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(trackedPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sent").value(true))
        .andExpect(jsonPath("$.email").value("quote.guest@example.com"))
        .andExpect(jsonPath("$.quoteNumber").value(quoteNumber))
        .andExpect(jsonPath("$.message").value(Matchers.containsString("quote.guest@example.com")));
    org.assertj.core.api.Assertions.assertThat(parkQuoteMailService.sentMails()).hasSize(1);
    com.ruzinn.dto.SentQuoteMail mail = parkQuoteMailService.sentMails().get(0);
    org.assertj.core.api.Assertions.assertThat(mail.to()).isEqualTo("quote.guest@example.com");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("Dubai Frame");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("QUOTATION");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("Amount (AED)");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("45.00");
    org.assertj.core.api.Assertions.assertThat(mail.html()).doesNotContain("AED 45.00");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("This is a quotation, not a confirmed booking");
    org.assertj.core.api.Assertions.assertThat(mail.html()).contains("The PDF copy is attached");
    org.assertj.core.api.Assertions.assertThat(mail.pdf()).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    org.assertj.core.api.Assertions.assertThat(new String(mail.pdf(), java.nio.charset.StandardCharsets.ISO_8859_1))
        .contains("Dubai Frame");
    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(trackedPayload))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", Matchers.containsString("Rathin-Quote-")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    org.assertj.core.api.Assertions.assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    org.assertj.core.api.Assertions.assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1))
        .contains("Dubai Frame")
        .contains("QUOTATION")
        .contains("Grand total")
        .contains("AMOUNT \\(AED\\)")
        .contains("45.00")
        .doesNotContain("AED 45.00");
    mockMvc
        .perform(
            post("/api/parks/quote/send")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"email":"not-an-email","quoteNumber":"%s","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                    """
                        .formatted(quoteNumber)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Enter a valid email address"));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from booking_main", Number.class).intValue())
        .isEqualTo(before.intValue());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from quote_main where quote_ref_num = ?", Number.class, quoteNumber)
                .intValue())
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select quote_status from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("SENT");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select user_id from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("admin@rathin.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select company_name from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("Rathin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select company_id from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("1");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select sent_to from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("quote.guest@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from quote_details where quote_ref_num = ?", Number.class, quoteNumber)
                .intValue())
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(quoteNumber).startsWith("QTE");

    mockMvc
        .perform(get("/api/company/quotes").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.quoteRefNum == '" + quoteNumber + "')].companyName", Matchers.hasItem("Rathin")));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEditItems").value(true))
        .andExpect(jsonPath("$.details[0].itemName").value(Matchers.containsString("Dubai Frame")))
        .andExpect(jsonPath("$.customerEmail").value("quote.guest@example.com"));
    mockMvc
        .perform(
            post("/api/company/quotes/" + quoteNumber + "/status")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PENDING\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/company/quotes/" + quoteNumber + "/follow-up")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"followUp\":\"Call customer tomorrow\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.followUp").value("Call customer tomorrow"));
    mockMvc
        .perform(get("/api/company/quotes").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.quoteRefNum == '" + quoteNumber + "')].status").value(Matchers.hasItem("PENDING")))
        .andExpect(
            jsonPath("$[?(@.quoteRefNum == '" + quoteNumber + "')].followUp")
                .value(Matchers.hasItem("Call customer tomorrow")));
    mockMvc
        .perform(get("/api/company/audit/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("QUOTE_MAIN"))
        .andExpect(jsonPath("$.entityId").value(quoteNumber));
  }

  @Test
  void acceptingAQuoteCreatesABookingOnlyAfterPayment() throws Exception {
    String payload =
        """
        {"adults":1,"children":0,"markupPercent":0,"email":"accept.guest@example.com","items":[
          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0},
          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-09-22","checkOut":"2026-09-23","adults":1,"children":0,"rooms":1,"roomType":"Standard"}
        ]}
        """;
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Number bookingsBefore = jdbc.queryForObject("select count(*) from booking_main", Number.class);
    bookingConfirmationMailService.clearSent();
    String quoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    String statusBefore =
        jdbc.queryForObject("select quote_status from quote_main where quote_ref_num = ?", String.class, quoteNumber);

    // Choosing ACCEPTED alone creates nothing: the quote is accepted only by a paid checkout.
    mockMvc
        .perform(
            post("/api/company/quotes/" + quoteNumber + "/status")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(Matchers.containsString("checkout")));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from booking_main", Number.class).intValue())
        .isEqualTo(bookingsBefore.intValue());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select quote_status from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo(statusBefore);
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).isEmpty();

    String acceptItems =
        """
        "items":[
          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0},
          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-09-22","checkOut":"2026-09-23","adults":1,"children":0,"rooms":1,"roomType":"Standard"}
        ]""";
    // A failed payment leaves the quote as it was.
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptQuoteCheckout(quoteNumber, acceptItems, "accept.guest@example.com", "123")
                    .replace("\"cardNumber\":\"4242424242424242\"", "\"cardNumber\":\"12\"")))
        .andExpect(status().isBadRequest());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from booking_main", Number.class).intValue())
        .isEqualTo(bookingsBefore.intValue());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select quote_status from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo(statusBefore);

    String confirmed =
        mockMvc
            .perform(
                post("/api/parks/confirm")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(acceptQuoteCheckout(quoteNumber, acceptItems, "accept.guest@example.com", "123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingRef").value(Matchers.startsWith("PRK")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String confirmedRef = com.jayway.jsonpath.JsonPath.read(confirmed, "$.bookingRef");

    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).hasSize(1);
    SentBookingMail acceptedMail = bookingConfirmationMailService.sentMails().get(0);
    org.assertj.core.api.Assertions.assertThat(acceptedMail.to()).isEqualTo("accept.guest@example.com");
    org.assertj.core.api.Assertions.assertThat(acceptedMail.html()).contains("BOOKING RECEIVED").contains("waiting for approval");
    org.assertj.core.api.Assertions.assertThat(acceptedMail.html()).contains("Dubai Frame");

    String bookingRef =
        jdbc.queryForObject(
            "select booking_ref_num from quote_main where quote_ref_num = ?", String.class, quoteNumber);
    org.assertj.core.api.Assertions.assertThat(bookingRef).isEqualTo(confirmedRef);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select quote_status from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo("ACCEPTED");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from booking_main", Number.class).intValue())
        .isEqualTo(bookingsBefore.intValue() + 1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from booking_main where booking_ref_num = ?", Number.class, bookingRef)
                .intValue())
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from booking_details where booking_ref_num = ?", Number.class, bookingRef)
                .intValue())
        .isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "select item_name from booking_details where booking_ref_num = ? order by id",
                String.class,
                bookingRef))
        .anyMatch(name -> name.contains("Dubai Frame"))
        .anyMatch(name -> name.contains("Sea View Hotel"));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select inventory_type from booking_details where booking_ref_num = ? and item_name like '%Sea View%'",
                String.class,
                bookingRef))
        .isEqualTo("HOTEL");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select booking_status from booking_main where booking_ref_num = ?", String.class, bookingRef))
        .isEqualTo("PENDING");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "select item_status from booking_details where booking_ref_num = ? order by id",
                String.class,
                bookingRef))
        .containsExactly("PENDING", "PENDING");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select booking_value from booking_main where booking_ref_num = ?", Number.class, bookingRef)
                .doubleValue())
        .isEqualTo(195.0);

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.bookingRefNum").value(bookingRef))
        .andExpect(jsonPath("$.canEditItems").value(false));
    mockMvc
        .perform(get("/api/company/bookings/" + bookingRef).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingRefNum").value(bookingRef))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.details[*].itemName", Matchers.hasItem(Matchers.containsString("Dubai Frame"))))
        .andExpect(jsonPath("$.details[*].itemName", Matchers.hasItem(Matchers.containsString("Sea View Hotel"))));
    mockMvc
        .perform(get("/api/parks/bookings/" + bookingRef).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.details.length()").value(2))
        .andExpect(jsonPath("$.details[0].status").value("PENDING"))
        .andExpect(jsonPath("$.details[1].status").value("PENDING"));
    mockMvc
        .perform(get("/api/company/quotes").session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.quoteRefNum == '" + quoteNumber + "')].bookingRefNum")
                .value(Matchers.hasItem(bookingRef)));

    mockMvc
        .perform(
            post("/api/company/quotes/" + quoteNumber + "/status")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(jsonPath("$.bookingRefNum").value(bookingRef));
    // A booked quote cannot be checked out (and charged) a second time.
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptQuoteCheckout(quoteNumber, acceptItems, "accept.guest@example.com", "123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(Matchers.containsString("already booked")));
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from booking_main", Number.class).intValue())
        .isEqualTo(bookingsBefore.intValue() + 1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select booking_ref_num from quote_main where quote_ref_num = ?", String.class, quoteNumber))
        .isEqualTo(bookingRef);

    // Staff accept the PENDING booking from the bookings list: only now the guest gets "confirmed".
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).hasSize(1);
    mockMvc
        .perform(
            post("/api/company/bookings/" + bookingRef + "/status")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).hasSize(2);
    SentBookingMail confirmedMail = bookingConfirmationMailService.sentMails().get(1);
    org.assertj.core.api.Assertions.assertThat(confirmedMail.to()).isEqualTo("accept.guest@example.com");
    org.assertj.core.api.Assertions.assertThat(confirmedMail.subject()).contains(bookingRef + " confirmed");
    org.assertj.core.api.Assertions.assertThat(confirmedMail.html())
        .contains("BOOKING CONFIRMATION")
        .contains("Your booking is confirmed")
        .contains("Dubai Frame")
        .doesNotContain("waiting for approval");
    // Saving CONFIRMED again does not send a second confirmation.
    mockMvc
        .perform(
            post("/api/company/bookings/" + bookingRef + "/status")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).hasSize(2);
  }

  /** Checkout body the UI sends when an accepted quote is paid for by card. */
  private static String acceptQuoteCheckout(String quoteNumber, String itemsJson, String email, String cvc) {
    return """
        {"quoteNumber":"%s","acceptQuote":true,"adults":1,"children":0,"markupPercent":0,%s,
         "leadName":"Accept Guest","email":"%s","phone":"+971500000555",
         "paymentMethod":"CARD","cardName":"Accept Guest","cardNumber":"4242424242424242",
         "cardExpiry":"09/28","cardCvc":"%s"}
        """
        .formatted(quoteNumber, itemsJson, email, cvc);
  }

  @Test
  void quoteItineraryCanBeEditedWithoutChangingQuoteNumber() throws Exception {
    MockHttpSession session = adminSession();
    String created =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"email":"edit.guest@example.com","items":[
                          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0},
                          {"itemId":"img-worlds-tickets-only","visitDate":"2026-09-22","adults":1,"children":0}
                        ]}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(created, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Number quotesBefore =
        jdbc.queryForObject(
            "select count(*) from quote_main where quote_ref_num = ?", Number.class, quoteNumber);

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(quoteNumber))
        .andExpect(jsonPath("$.canEditItems").value(true))
        .andExpect(jsonPath("$.details.length()").value(2))
        .andExpect(jsonPath("$.details[0].itemId").value("dubai-frame"))
        .andExpect(jsonPath("$.details[0].visitDate").value("2026-09-21"))
        .andExpect(jsonPath("$.details[1].itemId").value("img-worlds-tickets-only"))
        .andExpect(jsonPath("$.details[1].itemName").value(Matchers.containsString("IMG Worlds")));

    String edited =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"quoteNumber":"%s","items":[
                          {"itemId":"dubai-frame","visitDate":"2026-10-01","adults":1,"children":0},
                          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-02","checkOut":"2026-10-03","adults":1,"children":0,"rooms":1,"roomType":"Standard"}
                        ]}
                        """
                            .formatted(quoteNumber)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteNumber").value(quoteNumber))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat((String) com.jayway.jsonpath.JsonPath.read(edited, "$.quoteNumber"))
        .isEqualTo(quoteNumber);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from quote_main where quote_ref_num = ?", Number.class, quoteNumber)
                .intValue())
        .isEqualTo(quotesBefore.intValue());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "select item_id from quote_details where quote_ref_num = ? order by id",
                String.class,
                quoteNumber))
        .containsExactly("dubai-frame", "sea-view-hotel-bur-dubai");
    org.assertj.core.api.Assertions.assertThat(
            String.valueOf(
                jdbc.queryForObject(
                    "select visit_date from quote_details where quote_ref_num = ? and item_id = ?",
                    Object.class,
                    quoteNumber,
                    "dubai-frame")))
        .startsWith("2026-10-01");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                    "select count(*) from change_history where entity_id = ? and action = 'QUOTE_EDIT' and field_name = 'itinerary'",
                    Number.class,
                    quoteNumber)
                .intValue())
        .isGreaterThanOrEqualTo(1);

    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEditItems").value(true))
        .andExpect(jsonPath("$.details.length()").value(2))
        .andExpect(jsonPath("$.details[0].visitDate").value("2026-10-01"))
        .andExpect(jsonPath("$.details[1].itemId").value("sea-view-hotel-bur-dubai"))
        .andExpect(jsonPath("$.details[0].itemName").value(Matchers.containsString("Dubai Frame")))
        .andExpect(jsonPath("$.details[1].itemName").value(Matchers.containsString("Sea View Hotel")));

    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    acceptQuoteCheckout(
                        quoteNumber,
                        """
                        "items":[
                          {"itemId":"dubai-frame","visitDate":"2026-10-01","adults":1,"children":0},
                          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-02","checkOut":"2026-10-03","adults":1,"children":0,"rooms":1,"roomType":"Standard"}
                        ]""",
                        "edit.guest@example.com",
                        "123")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEditItems").value(false));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,"quoteNumber":"%s","items":[
                      {"itemId":"dubai-frame","visitDate":"2026-10-05","adults":1,"children":0}
                    ]}
                    """
                        .formatted(quoteNumber)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(Matchers.containsString("cannot be edited")));
  }

  @Test
  void quotesAreListedOnlyForTheLoggedInCompany() throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Quote Child Co",
                          "userName": "Quote Child Admin",
                          "email": "quote.child@example.com",
                          "phone": "+971500000777",
                          "password": "QuoteChild1"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));
    MockHttpSession childSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"quote.child@example.com\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());
    MockHttpSession superSession = adminSession();
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"quote child\",\"creditLimit\":\"500\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"quote.child@example.com\",\"password\":\"QuoteChild1\"}"))
        .andExpect(status().isOk());

    String childQuoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(childSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"email":"child.guest@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}]}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String childQuote = com.jayway.jsonpath.JsonPath.read(childQuoted, "$.quoteNumber");
    String parentQuoted =
        mockMvc
            .perform(
                post("/api/parks/quote")
                    .session(superSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"adults":1,"children":0,"markupPercent":0,"email":"parent.guest@example.com","items":[{"itemId":"dubai-frame","visitDate":"2026-09-22","adults":1,"children":0}]}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String parentQuote = com.jayway.jsonpath.JsonPath.read(parentQuoted, "$.quoteNumber");

    mockMvc
        .perform(get("/api/company/quotes").session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.hasItem(childQuote)))
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.not(Matchers.hasItem(parentQuote))))
        .andExpect(jsonPath("$[*].companyName", Matchers.hasItem("Quote Child Co")));
    mockMvc
        .perform(
            post("/api/company/quotes/" + parentQuote + "/status")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/company/quotes/" + childQuote + "/status")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PENDING\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/company/quotes/" + childQuote + "/follow-up")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"followUp\":\"Child follow-up note\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/quotes/" + childQuote).session(childSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(childQuote))
        .andExpect(jsonPath("$.canEditItems").value(true))
        .andExpect(jsonPath("$.details[0].itemName").value(Matchers.containsString("Dubai Frame")))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.followUp").value("Child follow-up note"));
    mockMvc
        .perform(get("/api/company/quotes/" + parentQuote).session(childSession))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/company/quotes").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.hasItem(parentQuote)))
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.not(Matchers.hasItem(childQuote))));
    mockMvc
        .perform(get("/api/company/quotes/" + childQuote).session(superSession))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/company/quotes?companyId=" + companyId).session(superSession))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/company/users")
                .session(childSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userName": "Quote Child Agent",
                      "email": "quote.agent@example.com",
                      "password": "QuoteAgent1",
                      "role": "COMPANY_USER"
                    }
                    """))
        .andExpect(status().isOk());
    MockHttpSession agentSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(agentSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"quote.agent@example.com\",\"password\":\"QuoteAgent1\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/quotes").session(agentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.hasItem(childQuote)))
        .andExpect(jsonPath("$[*].quoteRefNum", Matchers.not(Matchers.hasItem(parentQuote))))
        .andExpect(jsonPath("$[*].companyName", Matchers.hasItem("Quote Child Co")));
    mockMvc
        .perform(get("/api/company/quotes/" + childQuote).session(agentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteRefNum").value(childQuote))
        .andExpect(jsonPath("$.details[0].itemName").value(Matchers.containsString("Dubai Frame")))
        .andExpect(jsonPath("$.companyId").value(companyId));
  }

  @Test
  void quotesMultipleParksOnDifferentDates() throws Exception {
    String body =
        """
        {
          "adults": 2,
          "children": 1,
          "markupPercent": 5,
          "items": [
            {"itemId": "img-worlds-tickets-only", "visitDate": "2026-09-20", "adults": 2, "children": 0},
            {"itemId": "dubai-frame", "visitDate": "2026-09-21", "adults": 2, "children": 1},
            {"itemId": "sea-view-hotel-bur-dubai", "visitDate": "2026-09-22", "checkOut": "2026-09-23", "adults": 2, "children": 0, "rooms": 1, "roomType": "Standard"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("AED"))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.country").value("UAE"))
        .andExpect(jsonPath("$.subtotal").value(585.0))
        .andExpect(jsonPath("$.markupAmount").value(29.25))
        .andExpect(jsonPath("$.total").value(614.25))
        .andExpect(jsonPath("$.items[0].requiresPassengerDetails").value(false))
        .andExpect(jsonPath("$.items[0].bookingInfo").value("DFT"))
        .andExpect(jsonPath("$.items[0].passengerFields.length()").value(0))
        .andExpect(jsonPath("$.items[1].requiresPassengerDetails").value(false))
        .andExpect(jsonPath("$.items[2].requiresPassengerDetails").value(false));
  }

  @Test
  void mixedCountryQuoteIsRejectedAndSameCountryQuoteIsAllowed() throws Exception {
    mockMvc
        .perform(
            post("/api/parks/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adults": 2,
                      "children": 0,
                      "items": [
                        {"itemId": "dubai-frame", "visitDate": "2026-09-21", "adults": 2, "children": 0},
                        {"itemId": "ferrari-world", "visitDate": "2026-09-22", "adults": 2, "children": 0}
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.error")
                .value("Choose one country. This itinerary mixes items from different countries."));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "country": "UAE",
                      "adults": 2,
                      "children": 0,
                      "items": [
                        {"itemId": "ferrari-world", "visitDate": "2026-09-22", "adults": 2, "children": 0}
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Ferrari World is not available for United Arab Emirates"));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "country": "IND",
                      "adults": 2,
                      "children": 0,
                      "items": [
                        {"itemId": "ferrari-world", "visitDate": "2026-09-22", "adults": 2, "children": 0}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.country").value("IND"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.subtotal").value(560.0));
    mockMvc
        .perform(
            post("/api/parks/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "country": "SIN",
                      "adults": 1,
                      "children": 0,
                      "items": [
                        {"itemId": "transfer-half-day", "visitDate": "2026-09-22", "adults": 1, "children": 0, "timeSlot": "10:00"}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.country").value("SIN"))
        .andExpect(jsonPath("$.items[0].itemId").value("transfer-half-day"));
  }

  @Test
  void quotesAddOnParkRequiresPassengerDetails() throws Exception {
    String body =
        """
        {
          "adults": 1,
          "children": 0,
          "items": [
            {"itemId": "xline-dubai-marina", "visitDate": "2026-09-21", "adults": 1, "children": 0}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].bookingInfo").value("ADD_ON"))
        .andExpect(jsonPath("$.items[0].requiresPassengerDetails").value(true))
        .andExpect(jsonPath("$.items[0].passengerFields.length()").value(5));
  }

  @Test
  void quotesHotelRoomTypesAndVisaFromPriceList() throws Exception {
    String body =
        """
        {
          "adults": 2,
          "children": 0,
          "markupPercent": 0,
          "items": [
            {"itemId": "alaft-hotel-deira", "visitDate": "2026-09-22", "checkOut": "2026-09-23", "adults": 2, "children": 0, "rooms": 1, "roomType": "Suite"},
            {"itemId": "30-days-visa", "adults": 2, "children": 0}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].roomType").value("Suite"))
        .andExpect(jsonPath("$.items[0].nights").value(1))
        .andExpect(jsonPath("$.items[0].lineTotal").value(400.0))
        .andExpect(jsonPath("$.items[1].name").value("30 Days Visa"))
        .andExpect(jsonPath("$.items[1].lineTotal").value(600.0))
        .andExpect(jsonPath("$.total").value(1000.0));
  }

  @Test
  void quotesHotelStayAsOneNightForConsecutiveDates() throws Exception {
    String body =
        """
        {
          "adults": 2,
          "children": 0,
          "markupPercent": 0,
          "items": [
            {"itemId": "sea-view-hotel-bur-dubai", "visitDate": "2026-01-01", "checkOut": "2026-01-02", "adults": 2, "children": 0, "rooms": 1, "roomType": "Standard"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].nights").value(1))
        .andExpect(jsonPath("$.items[0].lineTotal").value(150.0))
        .andExpect(jsonPath("$.total").value(150.0));
  }

  @Test
  void quotesHotelStayAsNineNightsFromJan1ToJan10() throws Exception {
    String body =
        """
        {
          "adults": 2,
          "children": 0,
          "markupPercent": 0,
          "items": [
            {"itemId": "sea-view-hotel-bur-dubai", "visitDate": "2026-01-01", "checkOut": "2026-01-10", "adults": 2, "children": 0, "rooms": 1, "roomType": "Standard"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].nights").value(9))
        .andExpect(jsonPath("$.items[0].lineTotal").value(1350.0))
        .andExpect(jsonPath("$.total").value(1350.0));
  }

  @Test
  void rejectsHotelQuoteWhenCheckoutIsMissingOrNotAfterCheckIn() throws Exception {
    String missingCheckout =
        """
        {
          "adults": 2,
          "children": 0,
          "items": [
            {"itemId": "sea-view-hotel-bur-dubai", "visitDate": "2026-01-01", "adults": 2, "children": 0, "rooms": 1, "roomType": "Standard"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(missingCheckout))
        .andExpect(status().isBadRequest());

    String sameDay =
        """
        {
          "adults": 2,
          "children": 0,
          "items": [
            {"itemId": "sea-view-hotel-bur-dubai", "visitDate": "2026-01-01", "checkOut": "2026-01-01", "adults": 2, "children": 0, "rooms": 1, "roomType": "Standard"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(sameDay))
        .andExpect(status().isBadRequest());
  }

  @Test
  void fetchesLiveApiTimeslotsForDatedTypeDt() throws Exception {
    mockMvc
        .perform(
            get("/api/parks/timeslots")
                .param("itemId", "museum-of-the-future")
                .param("date", "2026-09-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemId").value("museum-of-the-future"))
        .andExpect(jsonPath("$.datedType").value("DT"))
        .andExpect(jsonPath("$.apiTimeSlot").value(true))
        .andExpect(jsonPath("$.source").value("api"))
        .andExpect(jsonPath("$.slots.length()").value(Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.slots[0].label").exists());
  }

  @Test
  void fetchesStaticTimeslotsWhenApiTimeSlotIsOff() throws Exception {
    mockMvc
        .perform(
            get("/api/parks/timeslots")
                .param("itemId", "sky-views-glass-slide-combo")
                .param("date", "2026-09-21"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiTimeSlot").value(false))
        .andExpect(jsonPath("$.source").value("static"))
        .andExpect(jsonPath("$.slots.length()").value(6));
  }

  @Test
  void rejectsTimeslotsForNonDtParks() throws Exception {
    mockMvc
        .perform(
            get("/api/parks/timeslots")
                .param("itemId", "ferrari-world")
                .param("date", "2026-09-20"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void persistsConfirmedBookingAndRetrievesOneToManyDetails() throws Exception {
    String body =
        """
        {
          "adults": 2,
          "children": 1,
          "markupPercent": 0,
          "leadName": "Chitaranjan Guest",
          "email": "chitu@example.com",
          "phone": "+971500000000",
          "userId": "USR-1001",
          "companyName": "Rathin Travel",
          "companyId": "WGT-B2B",
          "items": [
            {"itemId": "img-worlds-tickets-only", "visitDate": "2026-09-20", "adults": 2, "children": 1},
            {"itemId": "alaft-hotel-deira", "visitDate": "2026-09-22", "checkOut": "2026-09-23", "adults": 2, "children": 0, "rooms": 1, "roomType": "Suite"},
            {"itemId": "30-days-visa", "adults": 2, "children": 0}
          ]
        }
        """;
    String response =
        mockMvc
            .perform(post("/api/parks/book").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingRefNum").exists())
            .andExpect(jsonPath("$.userName").value("Chitaranjan Guest"))
            .andExpect(jsonPath("$.userId").value("USR-1001"))
            .andExpect(jsonPath("$.companyName").value("Rathin Travel"))
            .andExpect(jsonPath("$.companyId").value("WGT-B2B"))
            .andExpect(jsonPath("$.value").value(1480.0))
            .andExpect(jsonPath("$.currency").value("AED"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.createdDate").exists())
            .andExpect(jsonPath("$.details.length()").value(3))
            .andExpect(jsonPath("$.details[0].serviceId").value("img-worlds-tickets-only"))
            .andExpect(jsonPath("$.details[0].serviceName").value("Fantasy"))
            .andExpect(jsonPath("$.details[0].inventoryType").value("ATTRACTION"))
            .andExpect(jsonPath("$.details[0].showDate").value("2026-09-20"))
            .andExpect(jsonPath("$.details[0].adultCount").value(2))
            .andExpect(jsonPath("$.details[0].childCount").value(1))
            .andExpect(jsonPath("$.details[0].infantCount").value(0))
            .andExpect(jsonPath("$.details[0].toddlerCount").value(0))
            .andExpect(jsonPath("$.details[0].totalRate").value(480.0))
            .andExpect(jsonPath("$.details[0].status").value("PENDING"))
            .andExpect(jsonPath("$.details[0].supplierName").value("Rathin"))
            .andExpect(jsonPath("$.details[1].inventoryType").value("HOTEL"))
            .andExpect(jsonPath("$.details[1].itemName").value("Alaft Hotel - Deira — Suite"))
            .andExpect(jsonPath("$.details[1].propertyId").value("alaft-hotel-deira"))
            .andExpect(jsonPath("$.details[1].checkinDate").value("2026-09-22"))
            .andExpect(jsonPath("$.details[1].checkoutDate").value("2026-09-23"))
            .andExpect(jsonPath("$.details[1].totalRate").value(400.0))
            .andExpect(jsonPath("$.details[1].status").value("PENDING"))
            .andExpect(jsonPath("$.details[2].inventoryType").value("VISA"))
            .andExpect(jsonPath("$.details[2].serviceName").value("Visa"))
            .andExpect(jsonPath("$.details[2].totalRate").value(600.0))
            .andExpect(jsonPath("$.details[2].status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String bookingRef = com.jayway.jsonpath.JsonPath.read(response, "$.bookingRefNum");
    mockMvc
        .perform(get("/api/parks/bookings/" + bookingRef))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingRefNum").value(bookingRef))
        .andExpect(jsonPath("$.details.length()").value(3))
        .andExpect(jsonPath("$.details[0].bookingRefNum").value(bookingRef));
  }

  @Test
  void defaultsDirectCompanyWhenBookingWithoutAgencyFields() throws Exception {
    String body =
        """
        {
          "adults": 1,
          "children": 0,
          "markupPercent": 0,
          "leadName": "Walk In",
          "email": "walkin@example.com",
          "items": [
            {"itemId": "dubai-frame", "visitDate": "2026-09-21", "adults": 1, "children": 0}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/book").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("walkin@example.com"))
        .andExpect(jsonPath("$.companyName").value("Direct"))
        .andExpect(jsonPath("$.companyId").value("DIRECT"))
        .andExpect(jsonPath("$.details[0].companyName").value("Direct"));
  }

  @Test
  void returnsNotFoundForUnknownBookingRef() throws Exception {
    mockMvc
        .perform(get("/api/parks/bookings/PRKDOESNOTEX"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Booking not found: PRKDOESNOTEX"));
  }

  @Test
  void requiresTimeslotWhenQuotingDtPark() throws Exception {
    String missingSlot =
        """
        {
          "adults": 2,
          "children": 0,
          "items": [
            {"itemId": "museum-of-the-future", "visitDate": "2026-09-20", "adults": 2, "children": 0}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(missingSlot))
        .andExpect(status().isBadRequest());

    String withSlot =
        """
        {
          "adults": 2,
          "children": 0,
          "items": [
            {"itemId": "museum-of-the-future", "visitDate": "2026-09-20", "adults": 2, "children": 0, "timeSlot": "10:00"}
          ]
        }
        """;
    mockMvc
        .perform(post("/api/parks/quote").contentType(MediaType.APPLICATION_JSON).content(withSlot))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].timeSlot").value("10:00"));
  }

  @Test
  void processBookingWithCardConfirmsAndPersists() throws Exception {
    bookingConfirmationMailService.clearSent();
    String request =
        """
        {"adults":1,"children":0,"markupPercent":0,
         "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}],
         "leadName":"Ada Guest","email":"ada@example.com","phone":"+971500000222",
         "paymentMethod":"CARD","cardName":"Ada Guest","cardNumber":"4242424242424242",
         "cardExpiry":"09/28","cardCvc":"123",
         "userId":"HACK-USER","companyName":"Fake Agency"%s}
        """;
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.formatted(",\"companyId\":\"FAKE-ID\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Select a child company"));
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).isEmpty();
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.formatted("")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadName").value("Ada Guest"))
        .andExpect(jsonPath("$.paymentMethod").value("CARD"))
        .andExpect(jsonPath("$.cardLast4").value("4242"))
        .andExpect(jsonPath("$.quote.items[0].name").value("Dubai Frame"))
        .andExpect(jsonPath("$.quote.items[0].requiresPassengerDetails").value(false));
    org.assertj.core.api.Assertions.assertThat(bookingConfirmationMailService.sentMails()).hasSize(1);
    SentBookingMail bookingMail = bookingConfirmationMailService.sentMails().get(0);
    org.assertj.core.api.Assertions.assertThat(bookingMail.to()).isEqualTo("ada@example.com");
    org.assertj.core.api.Assertions.assertThat(bookingMail.html()).contains("BOOKING RECEIVED").contains("waiting for approval");
    org.assertj.core.api.Assertions.assertThat(bookingMail.subject()).contains("received, waiting for approval");
    org.assertj.core.api.Assertions.assertThat(bookingMail.html()).contains("Ada Guest");
    org.assertj.core.api.Assertions.assertThat(bookingMail.html()).contains("Paid by card");
    org.assertj.core.api.Assertions.assertThat(bookingMail.html()).contains("Dubai Frame");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_id FROM booking_main WHERE user_name = ?", String.class, "Ada Guest"))
        .isEqualTo("admin@rathin.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM booking_main WHERE user_name = ?", String.class, "Ada Guest"))
        .isEqualTo("Rathin");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM booking_main WHERE user_name = ?", String.class, "Ada Guest"))
        .isEqualTo("1");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT booking_status FROM booking_main WHERE user_name = ?", String.class, "Ada Guest"))
        .isEqualTo("PENDING");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForList(
                "SELECT d.item_status FROM booking_details d JOIN booking_main m ON d.booking_ref_num = m.booking_ref_num WHERE m.user_name = ?",
                String.class,
                "Ada Guest"))
        .containsOnly("PENDING");
  }

  @Test
  void processBookingSkyViewsComboReusesContactWithoutPassengerForms() throws Exception {
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cardConfirm(
                        "Sky Guest",
                        """
                        "adults": 4, "children": 0, "markupPercent": 0,
                        "items": [
                          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-10-01","checkOut":"2026-10-07","adults":4,"children":0,"rooms":2,"roomType":"Suite"},
                          {"itemId":"burj-khalifa-at-the-top-level-124-non-prime","visitDate":"2026-10-02","adults":4,"children":0,"timeSlot":"18:00"}
                        ]
                        """)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote.items[0].roomType").value("Suite"))
        .andExpect(jsonPath("$.quote.items[0].checkOut").value("2026-10-07"))
        .andExpect(jsonPath("$.quote.items[1].name").value("Burj Khalifa At The Top - Level 124 - Non-Prime"))
        .andExpect(jsonPath("$.quote.items[1].timeSlot").value("18:00"))
        .andExpect(jsonPath("$.quote.items[1].adults").value(4))
        .andExpect(jsonPath("$.quote.items[*].requiresPassengerDetails", Matchers.everyItem(Matchers.is(false))));
  }

  @Test
  void processBookingReusesCustomerContactForDftAndRequiresAddOnPassengers() throws Exception {
    String itinerary =
        """
        "adults": 1, "children": 0, "markupPercent": 0,
        "items": [
          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0},
          {"itemId":"xline-dubai-marina","visitDate":"2026-09-21","adults":1,"children":0%s}
        ]
        """;
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cardConfirm("Dana Guest", itinerary.formatted(""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(Matchers.containsString("Enter passenger details for XLine")));
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cardConfirm(
                        "Dana Guest",
                        itinerary.formatted(
                            ",\"passengers\":[{\"type\":\"ADULT\",\"firstName\":\"Sam\",\"lastName\":\"Rider\","
                                + "\"extra\":{\"passport\":\"P1234567\",\"dateOfBirth\":\"1994-04-12\",\"age\":\"32\"}}]"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote.items[0].requiresPassengerDetails").value(false))
        .andExpect(jsonPath("$.quote.items[0].passengers[0].firstName").value("Dana"))
        .andExpect(jsonPath("$.quote.items[0].passengers[0].email").value("guest@example.com"))
        .andExpect(jsonPath("$.quote.items[1].requiresPassengerDetails").value(true))
        .andExpect(jsonPath("$.quote.items[1].passengerFields[*].label", Matchers.hasItem("Passport number")));
  }

  @Test
  void processBookingHotelSkipsPassengerForms() throws Exception {
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cardConfirm(
                        "Hotel Guest",
                        """
                        "adults":2,"children":1,"markupPercent":0,
                        "items":[{"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-09-22","checkOut":"2026-09-24","adults":2,"children":1,"rooms":2,"roomType":"Standard"}]
                        """)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadName").value("Hotel Guest"))
        .andExpect(jsonPath("$.quote.items[0].name").value(Matchers.containsString("Sea View Hotel")))
        .andExpect(jsonPath("$.quote.items[0].visitDate").value("2026-09-22"))
        .andExpect(jsonPath("$.quote.items[0].checkOut").value("2026-09-24"))
        .andExpect(jsonPath("$.quote.items[0].nights").value(2))
        .andExpect(jsonPath("$.quote.items[0].rooms").value(2))
        .andExpect(jsonPath("$.quote.items[0].lineTotal").value(600.0))
        .andExpect(jsonPath("$.quote.items[0].adults").value(2))
        .andExpect(jsonPath("$.quote.items[0].children").value(1))
        .andExpect(jsonPath("$.quote.items[0].requiresPassengerDetails").value(false));
  }

  @Test
  void processBookingKeepsHotelAndDateWiseParkDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cardConfirm(
                        "Date Guest",
                        """
                        "adults": 5, "children": 0, "markupPercent": 0,
                        "items": [
                          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-09-23","checkOut":"2026-09-25","adults":2,"children":0,"rooms":2,"roomType":"Executive"},
                          {"itemId":"aya-universe-dubai","visitDate":"2026-09-24","adults":5,"children":0},
                          {"itemId":"burj-khalifa-at-the-top-level-124-non-prime","visitDate":"2026-09-24","adults":1,"children":0,"timeSlot":"10:00"}
                        ]
                        """)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote.items[0].roomType").value("Executive"))
        .andExpect(jsonPath("$.quote.items[0].nights").value(2))
        .andExpect(jsonPath("$.quote.items[0].lineTotal").value(800.0))
        .andExpect(jsonPath("$.quote.items[1].name").value("AYA Universe (Dubai)"))
        .andExpect(jsonPath("$.quote.items[1].visitDate").value("2026-09-24"))
        .andExpect(jsonPath("$.quote.items[1].adults").value(5))
        .andExpect(jsonPath("$.quote.items[2].name").value("Burj Khalifa At The Top - Level 124 - Non-Prime"))
        .andExpect(jsonPath("$.quote.items[2].timeSlot").value("10:00"))
        .andExpect(jsonPath("$.quote.items[*].requiresPassengerDetails", Matchers.everyItem(Matchers.is(false))));
  }

  @Test
  void processBookingCollectsAddOnFieldsForConfiguredParks() throws Exception {
    String itinerary =
        """
        "adults":1,"children":0,"markupPercent":0,
        "items":[{"itemId":"xline-dubai-marina","visitDate":"2026-09-21","adults":1,"children":0,
                  "passengers":[{"type":"ADULT","firstName":"Sam","lastName":"Rider"%s}]}]
        """;
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cardConfirm("XLine Guest", itinerary.formatted(""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(Matchers.containsString("is required for XLine")));
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(adminSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cardConfirm(
                        "XLine Guest",
                        itinerary.formatted(
                            ",\"extra\":{\"passport\":\"P1234567\",\"dateOfBirth\":\"1994-04-12\",\"age\":\"32\"}"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote.items[0].passengerFields[*].label", Matchers.hasItems("Passport number", "Date of birth", "Age")));
  }

  /** Confirm-request JSON: the given itinerary fields plus lead contact and a test card. */
  private static String cardConfirm(String leadName, String itineraryFields) {
    return "{"
        + itineraryFields
        + ",\"leadName\":\""
        + leadName
        + "\",\"email\":\"guest@example.com\",\"phone\":\"+971500000333\",\"paymentMethod\":\"CARD\","
        + "\"cardName\":\""
        + leadName
        + "\",\"cardNumber\":\"4242424242424242\",\"cardExpiry\":\"09/28\",\"cardCvc\":\"123\"}";
  }

  @Test
  void processBookingWithCompanyCreditChargesLimit() throws Exception {
    String registerBody =
        """
        {
          "companyName": "Credit Checkout Co",
          "userName": "Credit Admin",
          "email": "credit.checkout@example.com",
          "phone": "+971500000444",
          "password": "CreditPass1"
        }
        """;
    String registered =
        mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyId").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId =
        String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));

    MockHttpSession companySession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"credit.checkout@example.com\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk());

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Admin@rathin.com\",\"password\":\"R@t@2023\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"credit enabled\",\"creditLimit\":\"1000\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/login")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"credit.checkout@example.com\",\"password\":\"CreditPass1\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/company/credit").session(companySession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remaining").value(1000.0))
        .andExpect(jsonPath("$.canUseCredit").value(true));
    mockMvc
        .perform(
            post("/api/parks/confirm")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adults":1,"children":0,"markupPercent":0,
                     "items":[{"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0}],
                     "leadName":"Credit Admin","email":"credit.checkout@example.com","phone":"+971500000444",
                     "paymentMethod":"CREDIT"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentMethod").value("CREDIT"))
        .andExpect(jsonPath("$.leadName").value("Credit Admin"));
    mockMvc
        .perform(get("/api/company/credit").session(companySession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remaining").value(955.0));

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    java.math.BigDecimal used =
        jdbc.queryForObject(
            "SELECT credit_used FROM company WHERE company_id = ?",
            java.math.BigDecimal.class,
            Long.parseLong(companyId));
    org.assertj.core.api.Assertions.assertThat(used).isEqualByComparingTo("45.00");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT user_id FROM booking_main WHERE user_name = ?", String.class, "Credit Admin"))
        .isEqualTo("credit.checkout@example.com");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_name FROM booking_main WHERE user_name = ?",
                String.class,
                "Credit Admin"))
        .isEqualTo("Credit Checkout Co");
  }

  private static byte[] jpegLogo(String label) throws Exception {
    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D graphics = image.createGraphics();
    graphics.setColor(new java.awt.Color(0, 87, 184));
    graphics.fillRect(0, 0, 240, 80);
    graphics.setColor(java.awt.Color.WHITE);
    graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22));
    graphics.drawString(label, 16, 50);
    graphics.dispose();
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    if (!javax.imageio.ImageIO.write(image, "jpg", out)) {
      throw new IllegalStateException("Could not write JPEG logo for " + label);
    }
    return out.toByteArray();
  }

  @Test
  void quoteDefaultsExpireDateToSevenDaysAndKeepsEditedValue() throws Exception {
    String defaultExpire =
        java.time.LocalDate.now(com.ruzinn.util.BookingPresentation.DISPLAY_ZONE).plusDays(7).toString();
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
                          "country":"UAE",
                          "currency":"AED",
                          "items":[
                            {"itemId":"dubai-frame","visitDate":"2026-11-12","adults":1,"children":0}
                          ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expireDate").value(defaultExpire))
            .andExpect(jsonPath("$.expireDateLabel").value(com.ruzinn.util.BookingPresentation.formatTravel(
                java.time.LocalDate.parse(defaultExpire))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String quoteNumber = com.jayway.jsonpath.JsonPath.read(quoted, "$.quoteNumber");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT expire_date FROM quote_main WHERE quote_ref_num = ?",
                java.sql.Date.class,
                quoteNumber)
                .toLocalDate()
                .toString())
        .isEqualTo(defaultExpire);
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expireDate").value(defaultExpire));

    String customExpire = "2026-12-31";
    String customLabel = com.ruzinn.util.BookingPresentation.formatTravel(java.time.LocalDate.parse(customExpire));
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
                      "country":"UAE",
                      "currency":"AED",
                      "expireDate":"%s",
                      "items":[
                        {"itemId":"dubai-frame","visitDate":"2026-11-12","adults":1,"children":0}
                      ]
                    }
                    """
                        .formatted(quoteNumber, customExpire)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expireDate").value(customExpire))
        .andExpect(jsonPath("$.expireDateLabel").value(customLabel));
    mockMvc
        .perform(get("/api/company/quotes/" + quoteNumber).session(adminSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expireDate").value(customExpire));

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
                          "country":"UAE",
                          "expireDate":"%s",
                          "items":[
                            {"itemId":"dubai-frame","visitDate":"2026-11-12","adults":1,"children":0}
                          ]
                        }
                        """
                            .formatted(quoteNumber, customExpire)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String text = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(text)
        .contains("Rates are valid until " + customLabel)
        .doesNotContain("Rates are valid for 7 days");
  }
}

package com.ruzinn.account;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private javax.sql.DataSource dataSource;

  @Test
  void loginIsRequiredAndSignInContentIsPublic() throws Exception {
    mockMvc
        .perform(get("/api/login-home"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("GROUP BOOKINGS MADE SIMPLE")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Big Savings")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Book Group Deals")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Group Flights")));
    mockMvc.perform(get("/api/company/bookings")).andExpect(status().isUnauthorized());
    // Not signed in: "no user" rather than an error.
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/public/locations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.countries.length()").value(org.hamcrest.Matchers.greaterThan(200)))
        .andExpect(jsonPath("$.countries[0]").value("Afghanistan"))
        .andExpect(jsonPath("$.countries", org.hamcrest.Matchers.hasItems("United Arab Emirates", "Singapore", "Sri Lanka")))
        .andExpect(jsonPath("$.citiesByCountry['India']", org.hamcrest.Matchers.hasItem("Chennai")))
        .andExpect(jsonPath("$.citiesByCountry['Singapore']").doesNotExist());
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "wrong-password"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@123"))
        .andExpect(status().isUnauthorized());
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    mockMvc
        .perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@rathin.com"));
  }

  @Test
  void companyIdIsNumericForIndexing() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(columnType(jdbc, "company", "company_id"))
        .containsAnyOf("bigint", "int", "integer");
    org.assertj.core.api.Assertions.assertThat(columnType(jdbc, "app_user", "company_id"))
        .containsAnyOf("bigint", "int", "integer");
    org.assertj.core.api.Assertions.assertThat(columnType(jdbc, "company_registration", "company_id"))
        .containsAnyOf("bigint", "int", "integer");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM app_user WHERE lower(email) = ?",
                Long.class,
                "admin@rathin.com"))
        .isEqualTo(1L);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_id FROM company WHERE company_id = ?", Long.class, 1L))
        .isEqualTo(1L);
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "company"))
        .contains("company_type", "logo_path", "logo_file_name", "logo_content_type");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT company_type FROM company WHERE company_id = ?", String.class, 1L))
        .isEqualTo("PARENT");
  }

  @Test
  void companyRegistrationTableMatchesRegisterScreen() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(tableColumns(jdbc, "company_registration"))
        .containsExactlyInAnyOrder(
            "id",
            "company_id",
            "company_name",
            "registration_no",
            "address",
            "website",
            "country",
            "city",
            "zip_code",
            "owner_name",
            "owner_mobile",
            "owner_telephone",
            "owner_email",
            "operations_staff_name",
            "operations_staff_number",
            "finance_name",
            "finance_mobile",
            "finance_telephone",
            "finance_email",
            "vat_file_name",
            "vat_content_type",
            "vat_bytes",
            "licence_file_name",
            "licence_content_type",
            "licence_bytes",
            "otp_verified",
            "status",
            "created_date",
            "submitted_date");
  }

  @Test
  void webRegistrationRequiresCompanyProfileFields() throws Exception {
    mockMvc
        .perform(
            multipart("/api/auth/register")
                .param("companyName", "Coastal Travels")
                .param("userName", "Lina Owner")
                .param("email", "lina@coastal.example")
                .param("phone", "+971500000333")
                .param("password", "CoastalPass1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sendOtpAgainReusesTheUnverifiedRegistration() throws Exception {
    String email = "retry.owner@retrytours.example";
    String first = registerRetry(email, "Retry Tours");
    String second = registerRetry(email, "Retry Tours LLC"); // "Send OTP" again, details edited
    String companyId = String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(first, "$.companyId"));
    org.assertj.core.api.Assertions.assertThat(
            String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(second, "$.companyId")))
        .isEqualTo(companyId);

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM company WHERE lower(email) = ?", Integer.class, email))
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE lower(email) = ?", Integer.class, email))
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT company_name FROM company WHERE lower(email) = ?", String.class, email))
        .isEqualTo("Retry Tours LLC");

    MockHttpSession admin = new MockHttpSession();
    mockMvc.perform(apiLogin("Admin@rathin.com", "R@t@2023").session(admin)).andExpect(status().isOk());
    // Not verified yet: not in the admin company list.
    mockMvc
        .perform(get("/api/admin/companies/directory").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].company.email", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(email))));

    // Only the newest OTP works; after verifying, the company shows up and re-registering is a duplicate.
    String otp = com.jayway.jsonpath.JsonPath.read(second, "$.otp");
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(new MockHttpSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(email, otp)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/admin/companies/directory").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].company.email", org.hamcrest.Matchers.hasItem(email)));
    mockMvc
        .perform(retryRequest(email, "Retry Tours"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("An account already exists for " + email));
  }

  private String registerRetry(String email, String companyName) throws Exception {
    return mockMvc
        .perform(retryRequest(email, companyName))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder retryRequest(
      String email, String companyName) {
    return multipart("/api/auth/register-full")
        .param("companyName", companyName)
        .param("userName", "Retry Owner")
        .param("email", email)
        .param("phone", "+971500000777")
        .param("password", "RetryPass1")
        .param("registrationNo", "RT-1")
        .param("address", "Al Barsha")
        .param("country", "United Arab Emirates")
        .param("city", "Dubai")
        .param("zipCode", "00000")
        .param("ownerTelephone", "+97140000777")
        .param("operationsStaffName", "Ops")
        .param("operationsStaffNumber", "+971500000778")
        .param("financeName", "Fin")
        .param("financeMobile", "+971500000779")
        .param("financeTelephone", "+97140000778")
        .param("financeEmail", "fin@retrytours.example");
  }

  @Test
  void angularRegisterFullAcceptsAnyCountryWithTypedCity() throws Exception {
    // Same endpoint and fields the Angular register page sends ("Send OTP").
    mockMvc
        .perform(
            multipart("/api/auth/register-full")
                .param("companyName", "Lion City Tours")
                .param("userName", "Mei Owner")
                .param("email", "mei@lioncity.example")
                .param("phone", "+6581234567")
                .param("password", "LionCity1")
                .param("registrationNo", "SG-2026-01")
                .param("address", "1 Raffles Place")
                .param("website", "")
                .param("country", "Singapore")
                .param("city", "Singapore")
                .param("zipCode", "048616")
                .param("ownerTelephone", "+6561234567")
                .param("operationsStaffName", "Ops Desk")
                .param("operationsStaffNumber", "+6581234568")
                .param("financeName", "Finance Desk")
                .param("financeMobile", "+6581234569")
                .param("financeTelephone", "+6561234568")
                .param("financeEmail", "accounts@lioncity.example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("mei@lioncity.example"))
        .andExpect(jsonPath("$.status").value("PENDING"));

    // A country with a fixed city list still rejects a city outside it.
    mockMvc
        .perform(
            multipart("/api/auth/register-full")
                .param("companyName", "Wrong City Co")
                .param("userName", "Wrong Owner")
                .param("email", "owner@wrongcity.example")
                .param("phone", "+971500000999")
                .param("password", "WrongCity1")
                .param("registrationNo", "WC-1")
                .param("address", "Somewhere")
                .param("country", "United Arab Emirates")
                .param("city", "Atlantis")
                .param("zipCode", "0")
                .param("ownerTelephone", "+97140000000")
                .param("operationsStaffName", "Ops")
                .param("operationsStaffNumber", "+971500000998")
                .param("financeName", "Fin")
                .param("financeMobile", "+971500000997")
                .param("financeTelephone", "+97140000001")
                .param("financeEmail", "fin@wrongcity.example"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Select a valid city"));
  }

  @Test
  void registersCompanyFromWebFormWithProfileDocuments() throws Exception {
    MockHttpSession registerSession = new MockHttpSession();
    MockMultipartFile vat =
        new MockMultipartFile("vatCertificate", "vat.pdf", "application/pdf", "vat-cert".getBytes());
    MockMultipartFile licence =
        new MockMultipartFile("licence", "licence.pdf", "application/pdf", "trade-licence".getBytes());
    MockMultipartFile logo =
        new MockMultipartFile("logo", "coastal-logo.jpg", "image/jpeg", jpegLogo("COASTAL"));
    String registered =
            mockMvc
                .perform(
                    multipart("/api/auth/register")
                        .file(vat)
                        .file(licence)
                        .file(logo)
                        .param("companyName", "Coastal Travels")
                        .param("userName", "Lina Owner")
                        .param("email", "lina@coastal.example")
                        .param("phone", "+971500000333")
                        .param("password", "CoastalPass1")
                        .param("registrationNo", "CN-77881")
                        .param("address", "Sheikh Zayed Road")
                        .param("website", "https://coastal.example")
                        .param("country", "United Arab Emirates")
                        .param("city", "Dubai")
                        .param("zipCode", "00000")
                        .param("ownerTelephone", "+97143000001")
                        .param("operationsStaffName", "Sam Ops")
                        .param("operationsStaffNumber", "+971500000334")
                        .param("financeName", "Finance Desk")
                        .param("financeMobile", "+971500000335")
                        .param("financeTelephone", "+97143000002")
                        .param("financeEmail", "accounts@coastal.example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lina@coastal.example"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from company_registration where lower(owner_email) = ?",
                Integer.class,
                "lina@coastal.example"))
        .isZero();
    org.assertj.core.api.Assertions.assertThat(otp).isNotBlank();

    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(registerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"lina@coastal.example\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyStatus").value(org.hamcrest.Matchers.not("ACTIVE")));

    java.util.Map<String, Object> saved =
        jdbc.queryForMap("select * from company_registration where lower(owner_email) = ?", "lina@coastal.example");
    org.assertj.core.api.Assertions.assertThat(saved.get("company_name").toString()).isEqualTo("Coastal Travels");
    org.assertj.core.api.Assertions.assertThat(saved.get("registration_no").toString()).isEqualTo("CN-77881");
    org.assertj.core.api.Assertions.assertThat(saved.get("address").toString()).isEqualTo("Sheikh Zayed Road");
    org.assertj.core.api.Assertions.assertThat(saved.get("website").toString()).isEqualTo("https://coastal.example");
    org.assertj.core.api.Assertions.assertThat(saved.get("country").toString()).isEqualTo("United Arab Emirates");
    org.assertj.core.api.Assertions.assertThat(saved.get("city").toString()).isEqualTo("Dubai");
    org.assertj.core.api.Assertions.assertThat(saved.get("zip_code").toString()).isEqualTo("00000");
    org.assertj.core.api.Assertions.assertThat(saved.get("owner_name").toString()).isEqualTo("Lina Owner");
    org.assertj.core.api.Assertions.assertThat(saved.get("owner_mobile").toString()).isEqualTo("+971500000333");
    org.assertj.core.api.Assertions.assertThat(saved.get("owner_telephone").toString()).isEqualTo("+97143000001");
    org.assertj.core.api.Assertions.assertThat(saved.get("operations_staff_name").toString()).isEqualTo("Sam Ops");
    org.assertj.core.api.Assertions.assertThat(saved.get("operations_staff_number").toString()).isEqualTo("+971500000334");
    org.assertj.core.api.Assertions.assertThat(saved.get("finance_name").toString()).isEqualTo("Finance Desk");
    org.assertj.core.api.Assertions.assertThat(saved.get("finance_mobile").toString()).isEqualTo("+971500000335");
    org.assertj.core.api.Assertions.assertThat(saved.get("finance_telephone").toString()).isEqualTo("+97143000002");
    org.assertj.core.api.Assertions.assertThat(saved.get("finance_email").toString()).isEqualTo("accounts@coastal.example");
    org.assertj.core.api.Assertions.assertThat(saved.get("vat_file_name").toString()).isEqualTo("vat.pdf");
    org.assertj.core.api.Assertions.assertThat(saved.get("licence_file_name").toString()).isEqualTo("licence.pdf");
    org.assertj.core.api.Assertions.assertThat((byte[]) saved.get("vat_bytes")).isEqualTo("vat-cert".getBytes());
    org.assertj.core.api.Assertions.assertThat((byte[]) saved.get("licence_bytes")).isEqualTo("trade-licence".getBytes());
    org.assertj.core.api.Assertions.assertThat(saved.get("status").toString()).isEqualTo("SUBMITTED");
    org.assertj.core.api.Assertions.assertThat(saved.get("otp_verified")).isIn(true, Boolean.TRUE, 1);

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(superSession))
        .andExpect(status().isOk());

    String companies =
        mockMvc
            .perform(get("/api/admin/companies").session(superSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].registrationNo", org.hamcrest.Matchers.hasItem("CN-77881")))
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].city", org.hamcrest.Matchers.hasItem("Dubai")))
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].financeEmail", org.hamcrest.Matchers.hasItem("accounts@coastal.example")))
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].hasVatCertificate", org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].hasLicence", org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$[?(@.email=='lina@coastal.example')].hasLogo", org.hamcrest.Matchers.hasItem(true)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    java.util.List<?> companyIds =
        com.jayway.jsonpath.JsonPath.read(companies, "$[?(@.email=='lina@coastal.example')].companyId");
    String companyId = String.valueOf(companyIds.get(0));

    mockMvc
        .perform(get("/admin/companies/" + companyId + "/documents/vat").session(superSession))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("vat.pdf")))
        .andExpect(content().bytes("vat-cert".getBytes()));
    mockMvc
        .perform(get("/admin/companies/" + companyId + "/documents/licence").session(superSession))
        .andExpect(status().isOk())
        .andExpect(content().bytes("trade-licence".getBytes()));

    mockMvc
        .perform(get("/companies/" + companyId + "/logo").session(superSession))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(jpegLogo("COASTAL")));

    java.util.Map<String, Object> companyRow =
        jdbc.queryForMap("select logo_path, logo_file_name from company where company_id = ?", Long.valueOf(companyId));
    org.assertj.core.api.Assertions.assertThat(companyRow.get("logo_file_name").toString()).isEqualTo("coastal-logo.jpg");
    String storedPath = companyRow.get("logo_path").toString();
    org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.isRegularFile(java.nio.file.Path.of("target/test-uploads").resolve(storedPath)))
        .isTrue();
  }

  @Test
  void registersCompanyWithOtpThenSuperAdminActivatesAndUsersShareBookings() throws Exception {
    String registerBody =
        """
        {
          "companyName": "Desert Travels",
          "userName": "Aisha Admin",
          "email": "aisha@desert.example",
          "phone": "+971500000111",
          "password": "DesertPass1"
        }
        """;
    String registered =
        mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.companyId").isNumber())
            .andExpect(jsonPath("$.otp").exists())
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
                .content("{\"email\":\"aisha@desert.example\",\"otp\":\"" + otp + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.companyStatus").value("PENDING"));

    mockMvc
        .perform(get("/api/company/bookings").session(companySession))
        .andExpect(status().isUnauthorized());

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Admin@rathin.com\",\"password\":\"R@t@2023\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
        .andExpect(jsonPath("$.companyType").value("PARENT"));

    mockMvc
        .perform(
            post("/api/admin/companies/" + companyId + "/approve")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"KYC ok\",\"creditLimit\":\"5000\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.companyType").value("CHILD"));
    org.springframework.jdbc.core.JdbcTemplate companyHistory = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            companyHistory.queryForObject(
                "select count(*) from change_history where entity_type = 'COMPANY' and action = 'COMPANY_REVIEW' and entity_id = ?",
                Number.class,
                companyId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            companyHistory.queryForObject(
                "select count(*) from company_hstry where company_id = ?",
                Number.class,
                Long.valueOf(companyId))
                .intValue())
        .isGreaterThan(0);

    mockMvc
        .perform(
            post("/api/auth/login")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"aisha@desert.example\",\"password\":\"DesertPass1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.companyType").value("CHILD"));

    mockMvc
        .perform(
            post("/api/company/users")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userName": "Omar Agent",
                      "email": "omar@desert.example",
                      "password": "OmarPass12",
                      "role": "COMPANY_USER"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("COMPANY_USER"))
        .andExpect(jsonPath("$.companyId").value(Integer.valueOf(companyId)));

    String booked =
        mockMvc
        .perform(
            post("/api/parks/book")
                .session(companySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "adults": 1,
                      "children": 0,
                      "markupPercent": 0,
                      "leadName": "Aisha Admin",
                      "email": "aisha@desert.example",
                      "userId": "will-be-overwritten-if-blank",
                      "companyName": "Desert Travels",
                      "companyId": "%s",
                      "items": [
                        {"itemId": "dubai-frame", "visitDate": "2026-09-21", "adults": 1, "children": 0}
                      ]
                    }
                    """
                        .formatted(companyId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value(companyId))
        .andExpect(jsonPath("$.details.length()").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.details[0].status").value("PENDING"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String desertRef = com.jayway.jsonpath.JsonPath.read(booked, "$.bookingRefNum");
    Number desertItemIdNum = com.jayway.jsonpath.JsonPath.read(booked, "$.details[0].id");
    long desertItemId = desertItemIdNum.longValue();
    mockMvc
        .perform(itemStatus(desertRef, desertItemId, "CONFIRMED").session(superSession))
        .andExpect(status().isOk());

    MockHttpSession omarSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(omarSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"omar@desert.example\",\"password\":\"OmarPass12\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("COMPANY_USER"))
        .andExpect(jsonPath("$.companyType").value("CHILD"));

    mockMvc
        .perform(get("/api/company/bookings").session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].companyId").value(companyId))
        .andExpect(jsonPath("$[0].details[0].serviceId").value("dubai-frame"))
        .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingRefNum").value(desertRef))
        .andExpect(jsonPath("$.details[0].itemName").value(org.hamcrest.Matchers.containsString("Dubai Frame")));

    // Child company users can view but not change bookings.
    mockMvc
        .perform(itemStatus(desertRef, desertItemId, "PENDING").session(omarSession))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Only parent company users can update this booking."));
    org.springframework.jdbc.core.JdbcTemplate historyJdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            historyJdbc.queryForObject(
                "select count(*) from change_history where booking_ref_num = ? and action = 'STATUS_CHANGE'",
                Number.class,
                desertRef)
                .intValue())
        .isEqualTo(2);
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    mockMvc
        .perform(itemStatus(desertRef, desertItemId, "PENDING").session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));
    org.assertj.core.api.Assertions.assertThat(
            historyJdbc.queryForObject(
                "select count(*) from booking_details_hstry where id = ?",
                Number.class,
                desertItemId)
                .intValue())
        .isGreaterThan(0);
    mockMvc
        .perform(get("/api/company/audit/bookings/" + desertRef + "/items/" + desertItemId).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("BOOKING_DETAIL"))
        .andExpect(jsonPath("$.createdBy").value("Aisha Admin"))
        .andExpect(jsonPath("$.edits[0].what").value(org.hamcrest.Matchers.containsString("Status change")))
        .andExpect(jsonPath("$.edits[0].change").value("CONFIRMED → PENDING"))
        .andExpect(jsonPath("$.edits[0].who").value("Rathin Super Admin"))
        .andExpect(jsonPath("$.archivedRowCount").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(itemFollowUp(desertRef, desertItemId, "Waiting on supplier voucher").session(omarSession))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Only parent company users can update this booking."));
    mockMvc
        .perform(itemFollowUp(desertRef, desertItemId, "Waiting on supplier voucher").session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(itemStatus(desertRef, desertItemId, "CONFIRMED").session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.details[0].followUp").value("Waiting on supplier voucher"));
    org.springframework.jdbc.core.JdbcTemplate afterEdits = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            afterEdits.queryForObject(
                "select count(*) from change_history where booking_ref_num = ? and action = 'FOLLOW_UP'",
                Number.class,
                desertRef)
                .intValue())
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            afterEdits.queryForObject(
                "select new_value from change_history where booking_ref_num = ? and action = 'FOLLOW_UP'",
                String.class,
                desertRef))
        .isEqualTo("Waiting on supplier voucher");

    MockMultipartFile ticket =
        new MockMultipartFile("file", "desert-ticket.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 ticket".getBytes());
    String itemDocuments = "/api/company/bookings/" + desertRef + "/items/" + desertItemId + "/documents";
    mockMvc
        .perform(multipart(itemDocuments).file(ticket).param("kind", "ticket").session(omarSession))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(multipart(itemDocuments).file(ticket).param("kind", "ticket").session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            multipart(itemDocuments)
                .file(new MockMultipartFile("file", "desert-voucher.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 voucher".getBytes()))
                .param("kind", "voucher")
                .session(superSession))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Upload a ticket for this item"));
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(omarSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.details[0].ticketFileName").value("desert-ticket.pdf"))
        .andExpect(jsonPath("$.details[0].voucherFileName").value(org.hamcrest.Matchers.emptyOrNullString()));
    mockMvc
        .perform(get("/company/bookings/" + desertRef + "/items/" + desertItemId + "/documents/ticket").session(omarSession))
        .andExpect(status().isOk())
        .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("desert-ticket.pdf")))
        .andExpect(content().bytes("%PDF-1.4 ticket".getBytes()));
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    java.nio.file.Path storedPath =
        java.nio.file.Path.of(
            jdbc.queryForObject(
                "select file_path from booking_details where id = ?", String.class, desertItemId));
    org.assertj.core.api.Assertions.assertThat(storedPath.toString())
        .contains(desertRef.toLowerCase(java.util.Locale.ROOT))
        .contains("dubai-frame")
        .contains("item-" + desertItemId)
        .contains("ticket_desert-ticket.pdf");
    org.assertj.core.api.Assertions.assertThat(
            java.nio.file.Files.readAllBytes(java.nio.file.Path.of("target/test-uploads").resolve(storedPath)))
        .isEqualTo("%PDF-1.4 ticket".getBytes());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select ticket_bytes from booking_details where id = ?", byte[].class, desertItemId))
        .isNull();
    mockMvc
        .perform(get("/company/bookings/" + desertRef + "/documents/preferred").session(omarSession))
        .andExpect(status().isOk())
        .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("desert-ticket.pdf")));

    mockMvc
        .perform(get("/api/company/bookings").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].bookingRefNum", org.hamcrest.Matchers.hasItem(desertRef)));
    mockMvc
        .perform(get("/api/admin/companies/directory").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].company.companyId", org.hamcrest.Matchers.hasItem(Integer.valueOf(companyId))))
        .andExpect(jsonPath("$[*].company.companyName", org.hamcrest.Matchers.hasItem("Desert Travels")))
        .andExpect(
            jsonPath(
                "$[*].users[*].email",
                org.hamcrest.Matchers.hasItems("aisha@desert.example", "omar@desert.example")));

    mockMvc
        .perform(
            post("/api/company/users")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userName": "Weegoo Parent Agent",
                      "email": "parent.agent.desert@weegootour.test",
                      "password": "ParentPass1",
                      "role": "COMPANY_USER"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("COMPANY_USER"));

    MockHttpSession parentSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(parentSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"parent.agent.desert@weegootour.test\",\"password\":\"ParentPass1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("COMPANY_USER"))
        .andExpect(jsonPath("$.companyType").value("PARENT"));

    mockMvc
        .perform(get("/api/company/bookings").session(parentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].bookingRefNum", org.hamcrest.Matchers.hasItem(desertRef)));
    mockMvc
        .perform(get("/api/company/bookings/" + desertRef).session(parentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingRefNum").value(desertRef));
    mockMvc
        .perform(itemFollowUp(desertRef, desertItemId, "Parent agent follow-up").session(parentSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/audit/bookings/" + desertRef + "/items/" + desertItemId).session(parentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdBy").value("Aisha Admin"))
        .andExpect(jsonPath("$.edits[0].what").value(org.hamcrest.Matchers.containsString("Follow-up")))
        .andExpect(jsonPath("$.edits[0].change").value(org.hamcrest.Matchers.containsString("Parent agent follow-up")))
        .andExpect(jsonPath("$.edits[0].who").value("Weegoo Parent Agent"));
    mockMvc
        .perform(get("/api/company/users").session(parentSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].userName", org.hamcrest.Matchers.hasItem("Weegoo Parent Agent")));
  }

  @Test
  void superAdminBookingListShowsEveryCompanyWhileCompanyUsersStayScoped() throws Exception {
    String oasisBody =
        """
        {
          "companyName": "Oasis Tours",
          "userName": "Noor Admin",
          "email": "noor@oasis.example",
          "phone": "+971500000222",
          "password": "OasisPass1"
        }
        """;
    String oasisRegistered =
        mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(oasisBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String oasisOtp = com.jayway.jsonpath.JsonPath.read(oasisRegistered, "$.otp");
    String oasisCompanyId =
        String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(oasisRegistered, "$.companyId"));

    MockHttpSession oasisSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .session(oasisSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"noor@oasis.example\",\"otp\":\"" + oasisOtp + "\"}"))
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
            post("/api/admin/companies/" + oasisCompanyId + "/approve")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"ok\",\"creditLimit\":\"3000\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(oasisSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"noor@oasis.example\",\"password\":\"OasisPass1\"}"))
        .andExpect(status().isOk());

    String oasisBooked =
        mockMvc
            .perform(
                post("/api/parks/book")
                    .session(oasisSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "adults": 1,
                          "children": 0,
                          "markupPercent": 0,
                          "leadName": "Noor Admin",
                          "email": "noor@oasis.example",
                          "companyName": "Oasis Tours",
                          "companyId": "%s",
                          "items": [
                            {"itemId": "dubai-frame", "visitDate": "2020-01-10", "adults": 1, "children": 0}
                          ]
                        }
                        """
                            .formatted(oasisCompanyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String oasisRef = com.jayway.jsonpath.JsonPath.read(oasisBooked, "$.bookingRefNum");
    Number oasisItemIdNum = com.jayway.jsonpath.JsonPath.read(oasisBooked, "$.details[0].id");
    long oasisItemId = oasisItemIdNum.longValue();

    mockMvc
        .perform(get("/api/company/bookings").session(oasisSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].bookingRefNum", org.hamcrest.Matchers.hasItem(oasisRef)))
        .andExpect(jsonPath("$[*].companyId", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(oasisCompanyId))));

    mockMvc
        .perform(get("/api/company/bookings").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].bookingRefNum", org.hamcrest.Matchers.hasItem(oasisRef)));
    mockMvc
        .perform(get("/api/company/bookings").session(superSession).param("companyId", oasisCompanyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].bookingRefNum", org.hamcrest.Matchers.hasItem(oasisRef)))
        .andExpect(jsonPath("$[*].companyId", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(oasisCompanyId))));

    mockMvc
        .perform(get("/api/company/bookings/" + oasisRef).session(oasisSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.details[0].itemName").value(org.hamcrest.Matchers.containsString("Dubai Frame")));

    mockMvc
        .perform(
            multipart("/api/company/bookings/" + oasisRef + "/items/" + oasisItemId + "/documents")
                .file(
                    new MockMultipartFile(
                        "file", "oasis-ticket.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 oasis".getBytes()))
                .param("kind", "ticket")
                .session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/company/bookings/" + oasisRef).session(oasisSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.details[0].ticketFileName").value("oasis-ticket.pdf"));
    mockMvc
        .perform(get("/company/bookings/" + oasisRef + "/items/" + oasisItemId + "/documents/ticket").session(oasisSession))
        .andExpect(status().isOk())
        .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("oasis-ticket.pdf")))
        .andExpect(content().bytes("%PDF-1.4 oasis".getBytes()));

    String directory =
        mockMvc
            .perform(get("/api/company/bookings").session(superSession))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    java.util.List<String> otherRefs =
        com.jayway.jsonpath.JsonPath.read(
            directory, "$[?(@.companyId != '" + oasisCompanyId + "')].bookingRefNum");
    if (!otherRefs.isEmpty()) {
      String otherRef = otherRefs.get(0);
      mockMvc
          .perform(get("/api/company/bookings/" + otherRef).session(oasisSession))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(get("/company/bookings/" + otherRef + "/documents/voucher").session(oasisSession))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(
              post("/api/company/bookings/" + otherRef + "/status")
                  .session(oasisSession)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"status\":\"REJECTED\"}"))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(itemStatus(otherRef, 1L, "REJECTED").session(oasisSession))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(get("/company/bookings/" + otherRef + "/items/1/documents/voucher").session(oasisSession))
          .andExpect(status().isNotFound());
    }
  }

  @Test
  void superAdminCanBlockUsersAndResetPasswords() throws Exception {
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "companyName": "Harbor Travels",
                          "userName": "Nora Admin",
                          "email": "nora@harbor.example",
                          "phone": "+971500000444",
                          "password": "HarborPass1"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String otp = com.jayway.jsonpath.JsonPath.read(registered, "$.otp");
    String companyId =
        String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(registered, "$.companyId"));

    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nora@harbor.example\",\"otp\":\"" + otp + "\"}"))
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
                .content("{\"note\":\"KYC ok\",\"creditLimit\":\"2500\"}"))
        .andExpect(status().isOk());

    String createdUser =
        mockMvc
            .perform(
                post("/api/company/users")
                    .session(superSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "userName": "Rami Agent",
                          "email": "rami@harbor.example",
                          "password": "RamiPass12",
                          "role": "COMPANY_USER",
                          "companyId": "%s"
                        }
                        """
                            .formatted(companyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("rami@harbor.example"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = com.jayway.jsonpath.JsonPath.read(createdUser, "$.userId");

    mockMvc
        .perform(get("/api/admin/users").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.hasItem("rami@harbor.example")))
        .andExpect(jsonPath("$[?(@.email=='rami@harbor.example')].status", org.hamcrest.Matchers.hasItem("ACTIVE")));

    String superAdminId =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(get("/api/auth/me").session(superSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.userId");
    mockMvc
        .perform(post("/api/admin/users/" + superAdminId + "/block").session(superSession))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Super Admin accounts cannot be blocked"));
    mockMvc
        .perform(
            post("/api/admin/users/" + superAdminId + "/password")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"ChangedPass1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Super Admin passwords cannot be reset here"));

    MockHttpSession ramiSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(ramiSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rami@harbor.example\",\"password\":\"RamiPass12\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/admin/users/" + userId + "/block").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"));

    mockMvc
        .perform(get("/api/company/bookings").session(ramiSession))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("This user is disabled"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rami@harbor.example\",\"password\":\"RamiPass12\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("This user is disabled"));

    mockMvc
        .perform(get("/api/admin/users").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.email=='rami@harbor.example')].status", org.hamcrest.Matchers.hasItem("DISABLED")));

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from change_history where entity_type = 'APP_USER' and entity_id = ? and action = 'STATUS_CHANGE' and field_name = 'status'",
                Number.class,
                userId)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from app_user_hstry where user_id = ?",
                Number.class,
                userId)
                .intValue())
        .isGreaterThan(0);

    mockMvc
        .perform(post("/api/admin/users/" + userId + "/unblock").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/admin/users/" + userId + "/password")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"RamiPass99\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("rami@harbor.example"));

    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from change_history where entity_type = 'APP_USER' and entity_id = ? and action = 'PASSWORD_RESET'",
                Number.class,
                userId)
                .intValue())
        .isGreaterThan(0);
    java.util.List<java.util.Map<String, Object>> passwordHistory =
        jdbc.queryForList(
            "select old_value, new_value from change_history where entity_type = 'APP_USER' and entity_id = ? and action = 'PASSWORD_RESET'",
            userId);
    org.assertj.core.api.Assertions.assertThat(passwordHistory).isNotEmpty();
    for (java.util.Map<String, Object> row : passwordHistory) {
      org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("old_value")))
          .doesNotContain("RamiPass12")
          .doesNotContain("RamiPass99")
          .doesNotContain("RamiPass77");
      org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("new_value")))
          .isEqualTo("reset")
          .doesNotContain("RamiPass12")
          .doesNotContain("RamiPass99")
          .doesNotContain("RamiPass77");
    }

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rami@harbor.example\",\"password\":\"RamiPass12\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid email or password"));
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rami@harbor.example\",\"password\":\"RamiPass99\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("rami@harbor.example"));

    MockHttpSession noraSession = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/auth/login")
                .session(noraSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nora@harbor.example\",\"password\":\"HarborPass1\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/admin/users").session(noraSession))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/admin/users/" + userId + "/block")
                .session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            apiLogin("rami@harbor.example", "RamiPass99"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/admin/users/" + userId + "/unblock")
                .session(superSession))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/admin/users/" + userId + "/password")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"RamiPass77\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            apiLogin("rami@harbor.example", "RamiPass77"))
        .andExpect(status().isOk());
  }

  private static String columnType(JdbcTemplate jdbc, String table, String column) {
    String type =
        jdbc.queryForObject(
            """
            SELECT data_type
            FROM information_schema.columns
            WHERE lower(table_name) = ? AND lower(column_name) = ?
            LIMIT 1
            """,
            String.class,
            table.toLowerCase(java.util.Locale.ROOT),
            column.toLowerCase(java.util.Locale.ROOT));
    return type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
  }

  private static java.util.List<String> tableColumns(JdbcTemplate jdbc, String table) {
    return jdbc
        .queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE lower(table_name) = ?
            ORDER BY ordinal_position
            """,
            String.class,
            table.toLowerCase(java.util.Locale.ROOT))
        .stream()
        .map(name -> name.toLowerCase(java.util.Locale.ROOT))
        .toList();
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder itemStatus(
      String bookingRef, long itemId, String status) {
    return post("/api/company/bookings/" + bookingRef + "/items/" + itemId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"" + status + "\"}");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder itemFollowUp(
      String bookingRef, long itemId, String followUp) {
    return post("/api/company/bookings/" + bookingRef + "/items/" + itemId + "/follow-up")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"followUp\":\"" + followUp + "\"}");
  }

  private static byte[] jpegLogo(String label) throws Exception {
    BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(new Color(0, 87, 184));
    graphics.fillRect(0, 0, 240, 80);
    graphics.setColor(Color.WHITE);
    graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
    graphics.drawString(label, 16, 50);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "jpg", out)) {
      throw new IllegalStateException("Could not write JPEG logo for " + label);
    }
    return out.toByteArray();
  }
}

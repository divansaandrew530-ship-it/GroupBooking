package com.ruzinn.cms;

import static com.ruzinn.TestApi.apiLogin;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class MultilingualCmsTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private javax.sql.DataSource dataSource;

  @Test
  void superAdminManagesTranslationsAndPublicPagesFallBackToEnglish() throws Exception {
    mockMvc.perform(get("/api/admin/cms")).andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/api/i18n?lang=hi"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("साइन इन")))
        .andExpect(content().string(containsString("ईमेल")))
        .andExpect(content().string(containsString("मुझे याद रखें")))
        .andExpect(content().string(containsString("नियम")));
    mockMvc
        .perform(get("/api/login-home?lang=hi"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("ग्रुप फ्लाइट")))
        .andExpect(content().string(containsString("ग्रुप होटल")))
        .andExpect(content().string(containsString("स्थानीय आकर्षण")))
        .andExpect(content().string(containsString("समर्पित सहायता")))
        .andExpect(content().string(containsString("ग्रुप बुकिंग अब आसान")));

    mockMvc
        .perform(get("/api/i18n?lang=ar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("تسجيل الدخول")))
        .andExpect(content().string(containsString("البريد الإلكتروني")))
        .andExpect(content().string(containsString("تذكرني")));
    mockMvc
        .perform(get("/api/login-home?lang=ar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("رحلات جماعية")));

    mockMvc
        .perform(get("/api/i18n?lang=ru"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Эл. почта")));
    mockMvc
        .perform(get("/api/login-home?lang=ru"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Вход")))
        .andExpect(content().string(containsString("Групповые рейсы")));

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(superSession))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/auth/me").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@rathin.com"));

    mockMvc
        .perform(get("/api/i18n?lang=en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Languages")))
        .andExpect(content().string(containsString("Process Booking")));

    mockMvc
        .perform(get("/api/i18n?lang=hi"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("संपादित करें और कोट")))
        .andExpect(content().string(containsString("बुकिंग प्रोसेस करें")))
        .andExpect(content().string(containsString("मल्टी-पार्क बुकिंग")))
        .andExpect(content().string(containsString("आइटम")))
        .andExpect(content().string(containsString("पिछले 30 दिनों की बुकिंग")))
        .andExpect(content().string(containsString("मल्टी-पार्क")));

    mockMvc
        .perform(get("/api/admin/cms?lang=hi").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lang").value("hi"))
        .andExpect(jsonPath("$.rows[?(@.key == 'nav.quotes')].value", hasItem("कोट")));

    mockMvc
        .perform(
            post("/api/admin/cms")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lang": "hi",
                      "values": {
                        "nav.quotes": "कोटेशन CMS",
                        "nav.items": "आइटम",
                        "nav.cms": "भाषाएँ",
                        "login.submit": "",
                        "booking.process": "बुकिंग प्रोसेस करें",
                        "booking.editAndQuote": "संपादित करें और कोट",
                        "booking.quote": "कोट",
                        "booking.summary": "बुकिंग सारांश",
                        "nav.dashboard": "डैशबोर्ड",
                        "nav.bookings": "बुकिंग",
                        "nav.companies": "कंपनियाँ",
                        "nav.users": "उपयोगकर्ता",
                        "nav.loginHome": "लॉगिन होम",
                        "nav.newBooking": "नई बुकिंग",
                        "nav.signIn": "साइन इन",
                        "nav.register": "पंजीकरण",
                        "nav.signOut": "साइन आउट",
                        "login.email": "ईमेल",
                        "login.password": "पासवर्ड",
                        "cms.save": "अनुवाद सहेजें",
                        "cms.uiStrings": "स्क्रीन लेबल",
                        "cms.loginHome": "लॉगिन होम",
                        "cms.items": "आइटम सामग्री",
                        "cms.fallbackHint": "खाली मान अंग्रेज़ी पर वापस जाते हैं।"
                      }
                    }
                    """))
        .andExpect(status().isOk());

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select text_value from cms_translation where bundle = 'UI' and field_name = 'nav.quotes' and locale = 'hi'",
                String.class))
        .isEqualTo("कोटेशन CMS");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from cms_translation_hstry", Number.class).intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from change_history where entity_type = 'CMS_TRANSLATION' and action = 'CMS_SAVE'",
                Number.class)
                .intValue())
        .isGreaterThan(0);

    mockMvc
        .perform(get("/api/i18n?lang=hi"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$['nav.quotes']").value("कोटेशन CMS"))
        .andExpect(jsonPath("$['login.submit']").value("Sign In"))
        .andExpect(jsonPath("$['nav.signIn']").value("साइन इन"));

    mockMvc
        .perform(get("/api/admin/login-home?lang=hi").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productPill").value("B2B कंपनी ग्रुप बुकिंग"))
        .andExpect(jsonPath("$.structureLocked").value(true));

    mockMvc
        .perform(
            post("/api/admin/login-home")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lang": "hi",
                      "productName": "GROUPBOOKINGLA",
                      "productPill": "हिंदी CMS पिल",
                      "panelTagline": "साथ यात्रा करें CMS",
                      "signInTitle": "साइन इन",
                      "signInSubtitle": "हिंदी CMS उपशीर्षक",
                      "registerLabel": "पंजीकरण",
                      "footerCopy": "© 2026 Rathin. सर्वाधिकार सुरक्षित।"
                    }
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/login-home?lang=hi"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productPill").value("हिंदी CMS पिल"))
        .andExpect(jsonPath("$.panelTagline").value("साथ यात्रा करें CMS"));

    mockMvc
        .perform(get("/api/login-home?lang=en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productPill").value("B2B company group booking"));

    String created =
        mockMvc
            .perform(
                post("/api/admin/items")
                    .session(superSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemName": "CMS Desert Safari",
                          "datedType": "DA",
                          "category": "Park",
                          "adultRate": "100",
                          "childRate": "50",
                          "bookingInfo": "DFT",
                          "description": "English desert description",
                          "termsAndCondition": "English terms",
                          "included": "English included",
                          "excluded": "English excluded"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String itemId = JsonPath.read(created, "$.itemId");

    mockMvc
        .perform(
            put("/api/admin/items/" + itemId + "/translations/hi")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "itemName": "सीएमएस डेजर्ट सफारी",
                      "description": "हिंदी विवरण",
                      "termsAndCondition": "हिंदी शर्तें",
                      "included": "हिंदी शामिल",
                      "excluded": "हिंदी शामिल नहीं"
                    }
                    """))
        .andExpect(status().isOk());

    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select item_name from item_master where item_id = ?", String.class, itemId))
        .isEqualTo("CMS Desert Safari");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select text_value from cms_translation where bundle = 'ITEM_MASTER' and entity_id = ? and field_name = 'description' and locale = 'hi'",
                String.class,
                itemId))
        .isEqualTo("हिंदी विवरण");

    mockMvc
        .perform(get("/api/admin/items/" + itemId + "/translations/hi").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemName").value("सीएमएस डेजर्ट सफारी"))
        .andExpect(jsonPath("$.description").value("हिंदी विवरण"));
  }
}

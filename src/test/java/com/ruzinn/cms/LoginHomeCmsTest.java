package com.ruzinn.cms;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginHomeCmsTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private javax.sql.DataSource dataSource;

  @Test
  void superAdminCanEditLoginHomeAndPublicPageShowsIt() throws Exception {
    mockMvc.perform(get("/api/admin/login-home")).andExpect(status().isUnauthorized());

    MockHttpSession superSession = new MockHttpSession();
    mockMvc
        .perform(
            apiLogin("Admin@rathin.com", "R@t@2023").session(superSession))
        .andExpect(status().isOk());

    String editor =
        mockMvc
            .perform(get("/api/admin/login-home").session(superSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productName").exists())
            .andExpect(jsonPath("$.slides[0].id").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firstSlideId = String.valueOf(((Number) JsonPath.read(editor, "$.slides[0].id")).longValue());

    mockMvc
        .perform(get("/login-home/media/slides/" + firstSlideId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("image/jpeg"));

    mockMvc
        .perform(
            multipart("/api/admin/login-home/slides/" + firstSlideId + "/image")
                .file(new MockMultipartFile("file", "hero.jpg", "image/jpeg", jpegBytes()))
                .session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value("/login-home/media/slides/" + firstSlideId));

    mockMvc
        .perform(
            post("/api/admin/login-home")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveRequest(firstSlideId, "Travel Together CMS", "CMS Savings for Bigger Journeys")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.panelTagline").value("Travel Together CMS"));

    org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select count(*) from change_history where entity_type = 'LOGIN_HOME' and action = 'CMS_SAVE'",
                Number.class)
                .intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select new_value from change_history where field_name = 'panel_tagline' order by id desc limit 1",
                String.class))
        .isEqualTo("Travel Together CMS");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select new_value from change_history where field_name = 'slide_title' order by id desc limit 1",
                String.class))
        .isEqualTo("CMS Savings for Bigger Journeys");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from login_home_page_hstry", Number.class).intValue())
        .isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from login_home_slide_hstry", Number.class).intValue())
        .isGreaterThan(0);

    mockMvc
        .perform(get("/api/admin/audit/login-home").session(superSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entityType").value("LOGIN_HOME"))
        .andExpect(jsonPath("$.createdBy").value("System"))
        .andExpect(jsonPath("$.edits[0].what").value(org.hamcrest.Matchers.containsString("Login home")))
        .andExpect(jsonPath("$.archivedRowCount").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(get("/api/login-home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.panelTagline").value("Travel Together CMS"))
        .andExpect(jsonPath("$.slides[0].title").value(org.hamcrest.Matchers.containsString("CMS Savings")))
        .andExpect(jsonPath("$.slides[0].ctaLabel").value("Book Group Deals"))
        .andExpect(jsonPath("$.slides[0].imageUrl").value("/login-home/media/slides/" + firstSlideId));

    mockMvc
        .perform(get("/login-home/media/slides/" + firstSlideId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("image/jpeg"));

    mockMvc
        .perform(
            post("/api/admin/login-home")
                .session(superSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    saveRequest(
                        firstSlideId,
                        "Travel Together  •  Better Business  •  Greater Possibilities",
                        "Big Savings for Bigger Journeys")))
        .andExpect(status().isOk());
  }

  private static String saveRequest(String slideId, String panelTagline, String slideTitle) {
    return """
        {
          "productName": "GROUPBOOKINGLA",
          "productPill": "B2B company group booking",
          "panelTagline": "%s",
          "signInTitle": "Sign In",
          "signInSubtitle": "Please enter your credentials to sign in!",
          "registerLabel": "Register",
          "footerCopy": "© 2026 Rathin. All rights reserved.",
          "termsHref": "#terms",
          "privacyHref": "#privacy",
          "supportHref": "#support",
          "slides": [{
            "id": %s,
            "sortOrder": 1,
            "enabled": true,
            "eyebrow": "GROUP BOOKINGS MADE SIMPLE",
            "title": "%s",
            "accentPhrase": "for Bigger",
            "scriptText": "Travel More Together",
            "badgeKicker": "UP TO",
            "badgeValue": "30%% OFF",
            "badgeSub": "GROUP BOOKINGS",
            "body": "Exclusive group fares on flights, hotels and activities worldwide.",
            "ctaLabel": "Book Group Deals",
            "ctaHref": "/register"
          }]
        }
        """
        .formatted(panelTagline, slideId, slideTitle);
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

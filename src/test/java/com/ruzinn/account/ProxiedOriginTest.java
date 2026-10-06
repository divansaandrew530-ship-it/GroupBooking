package com.ruzinn.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Production runs behind nginx/Cloudflare, which terminate HTTPS and forward plain HTTP to the app.
 * A browser on the site sends its own https Origin; the app must treat that as same-origin.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProxiedOriginTest {

  private static final String SITE = "groupbooking.chitaranjan.com";

  @Autowired private MockMvc mockMvc;

  @Test
  void sameSiteLoginBehindHttpsProxyIsNotRejectedAsCors() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .header("Host", SITE)
                .header("Origin", "https://" + SITE)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", SITE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void otherSitesAreStillRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .header("Host", SITE)
                .header("Origin", "https://evil.example.com")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", SITE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isForbidden());
  }
}

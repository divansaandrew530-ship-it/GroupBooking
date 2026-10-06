package com.ruzinn.service;

import com.ruzinn.config.MailProperties;
import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.exception.MailDeliveryException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Microsoft Graph delivery (POST /users/{from}/sendMail) with an app-only OAuth token. Works when
 * SMTP AUTH is disabled for the Microsoft 365 tenant. Needs an Entra ID app registration with the
 * Mail.Send application permission (admin consent granted); the From mailbox must exist in the tenant.
 */
@Component
public class GraphMailTransport implements MailTransport {

  private static final Logger log = LoggerFactory.getLogger(GraphMailTransport.class);

  private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
  private static final String LOGIN_BASE = "https://login.microsoftonline.com";
  /** Graph rejects sendMail requests over 4MB; base64 adds a third, so keep raw attachments under 3MB. */
  private static final int MAX_ATTACHMENT_BYTES = 3 * 1024 * 1024;
  private static final int MAX_ATTEMPTS = 3;

  private final MailProperties mailProperties;
  private final ObjectMapper objectMapper;
  private final HttpClient http;

  private String accessToken;
  private Instant accessTokenExpiresAt = Instant.EPOCH;

  public GraphMailTransport(MailProperties mailProperties, ObjectMapper objectMapper) {
    this.mailProperties = mailProperties;
    this.objectMapper = objectMapper;
    this.http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(mailProperties.getConnectTimeoutMs()))
            .build();
  }

  @Override
  public void send(OutgoingMail mail) throws Exception {
    if (mail.hasAttachment() && mail.attachment().length > MAX_ATTACHMENT_BYTES) {
      throw new MailDeliveryException(
          "Attachment " + mail.attachmentNameOrDefault() + " is too large for Microsoft Graph ("
              + mail.attachment().length + " bytes, limit " + MAX_ATTACHMENT_BYTES + ")");
    }
    String from = mailProperties.getFrom();
    String body = objectMapper.writeValueAsString(sendMailBody(mail));
    URI uri =
        URI.create(GRAPH_BASE + "/users/" + URLEncoder.encode(from, StandardCharsets.UTF_8) + "/sendMail");

    for (int attempt = 1; ; attempt++) {
      HttpRequest request =
          HttpRequest.newBuilder(uri)
              .timeout(Duration.ofMillis(mailProperties.getReadTimeoutMs()))
              .header("Authorization", "Bearer " + token(false))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response;
      try {
        response = http.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (IOException ex) {
        if (attempt < MAX_ATTEMPTS) {
          log.warn("Graph sendMail attempt {} failed ({}); retrying", attempt, ex.getMessage());
          pause(attempt, null);
          continue;
        }
        throw new MailDeliveryException("Microsoft Graph unreachable: " + ex.getMessage(), ex);
      }
      int status = response.statusCode();
      if (status == 202 || status == 200) {
        return;
      }
      if (status == 401 && attempt < MAX_ATTEMPTS) {
        // Token revoked or expired early: fetch a new one and retry.
        token(true);
        continue;
      }
      if ((status == 429 || status >= 500) && attempt < MAX_ATTEMPTS) {
        log.warn("Graph sendMail attempt {} got HTTP {}; retrying", attempt, status);
        pause(attempt, response.headers().firstValue("Retry-After").orElse(null));
        continue;
      }
      throw new MailDeliveryException(
          "Microsoft Graph sendMail failed: HTTP " + status + " " + graphError(response.body()));
    }
  }

  private Map<String, Object> sendMailBody(OutgoingMail mail) {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("subject", mail.subject());
    message.put("body", Map.of("contentType", "HTML", "content", mail.html()));
    message.put(
        "from",
        Map.of("emailAddress", Map.of("address", mailProperties.getFrom(), "name", mailProperties.getFromName())));
    List<Map<String, Object>> to = new ArrayList<>();
    for (String address : mail.to().split("[,;]")) {
      if (!address.isBlank()) {
        to.add(Map.of("emailAddress", Map.of("address", address.trim())));
      }
    }
    message.put("toRecipients", to);
    if (mail.hasAttachment()) {
      Map<String, Object> file = new LinkedHashMap<>();
      file.put("@odata.type", "#microsoft.graph.fileAttachment");
      file.put("name", mail.attachmentNameOrDefault());
      file.put("contentType", mail.attachmentContentTypeOrDefault());
      file.put("contentBytes", Base64.getEncoder().encodeToString(mail.attachment()));
      message.put("attachments", List.of(file));
    }
    return Map.of("message", message, "saveToSentItems", true);
  }

  private synchronized String token(boolean forceRefresh) throws Exception {
    if (!forceRefresh && accessToken != null && Instant.now().isBefore(accessTokenExpiresAt)) {
      return accessToken;
    }
    MailProperties.Graph graph = mailProperties.getGraph();
    String form =
        "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(graph.getClientId(), StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(graph.getClientSecret(), StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode("https://graph.microsoft.com/.default", StandardCharsets.UTF_8);
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    LOGIN_BASE + "/" + URLEncoder.encode(graph.getTenantId(), StandardCharsets.UTF_8)
                        + "/oauth2/v2.0/token"))
            .timeout(Duration.ofMillis(mailProperties.getReadTimeoutMs()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      accessToken = null;
      throw new MailDeliveryException(
          "Microsoft sign-in for Graph failed: HTTP " + response.statusCode() + " " + tokenError(response.body()));
    }
    JsonNode json = objectMapper.readTree(response.body());
    accessToken = json.path("access_token").asText();
    long expiresIn = json.path("expires_in").asLong(3600);
    // Renew a few minutes early so a token never expires mid-request.
    accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
    log.info("Microsoft Graph access token obtained (valid {} s)", expiresIn);
    return accessToken;
  }

  private String graphError(String body) {
    try {
      JsonNode error = objectMapper.readTree(body).path("error");
      return error.path("code").asText("") + ": " + error.path("message").asText(body);
    } catch (Exception ex) {
      return body;
    }
  }

  private String tokenError(String body) {
    try {
      JsonNode json = objectMapper.readTree(body);
      return json.path("error").asText("") + ": " + json.path("error_description").asText(body);
    } catch (Exception ex) {
      return body;
    }
  }

  private static void pause(int attempt, String retryAfter) throws InterruptedException {
    long millis = 1000L * attempt;
    if (retryAfter != null) {
      try {
        millis = Math.min(10_000L, Long.parseLong(retryAfter.trim()) * 1000L);
      } catch (NumberFormatException ignored) {
        // Keep the default back-off.
      }
    }
    Thread.sleep(millis);
  }
}

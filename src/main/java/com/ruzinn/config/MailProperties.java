package com.ruzinn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ruzinn.mail")
public class MailProperties {

  /** Mail providers: smtp, graph (Microsoft Graph API), log (record only), or auto (see {@link #effectiveProvider}). */
  public static final String PROVIDER_AUTO = "auto";
  public static final String PROVIDER_SMTP = "smtp";
  public static final String PROVIDER_GRAPH = "graph";
  public static final String PROVIDER_LOG = "log";

  private String provider = PROVIDER_AUTO;
  private String fromName = "Rathin";
  private int connectTimeoutMs = 15000;
  private int readTimeoutMs = 30000;
  private final Graph graph = new Graph();

  private String host = "";
  private int port = 587;
  private String username = "";
  private String password = "";
  private String from = "ruzinn123@gmail.com";
  private boolean starttls = true;
  private boolean ssl = false;
  private String siteUrl = "https://13.202.214.183/";

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public boolean isStarttls() {
    return starttls;
  }

  public void setStarttls(boolean starttls) {
    this.starttls = starttls;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public String getSiteUrl() {
    return siteUrl;
  }

  public void setSiteUrl(String siteUrl) {
    this.siteUrl = siteUrl;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public void setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
  }

  public int getReadTimeoutMs() {
    return readTimeoutMs;
  }

  public void setReadTimeoutMs(int readTimeoutMs) {
    this.readTimeoutMs = readTimeoutMs;
  }

  public Graph getGraph() {
    return graph;
  }

  public boolean smtpConfigured() {
    return host != null && !host.isBlank();
  }

  /**
   * The provider actually used. "auto" picks Microsoft Graph when its app credentials are set, else
   * SMTP when a host is set, else log-only.
   */
  public String effectiveProvider() {
    String chosen = provider == null ? PROVIDER_AUTO : provider.trim().toLowerCase();
    if (!PROVIDER_AUTO.equals(chosen) && !chosen.isEmpty()) {
      return chosen;
    }
    if (graph.configured()) {
      return PROVIDER_GRAPH;
    }
    return smtpConfigured() ? PROVIDER_SMTP : PROVIDER_LOG;
  }

  /** Microsoft Graph (Entra ID app registration with the Mail.Send application permission). */
  public static class Graph {

    private String tenantId = "258a1dc4-bbee-4c3d-97d1-d85d76eaf657";
    private String clientId = "d6421e3b-1fad-4d40-85ff-a72764c7b066";
    private String clientSecret = "";

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public boolean configured() {
      return notBlank(tenantId) && notBlank(clientId) && notBlank(clientSecret);
    }

    /** Secret IDs are GUIDs; real secret values (e.g. "Xy48Q~...") never are. */
    public boolean clientSecretLooksLikeSecretId() {
      return clientSecret != null
          && clientSecret.trim().matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    /** Safe-to-log form of the secret: first 3 characters and its length, never the whole value. */
    public String maskedClientSecret() {
      if (!notBlank(clientSecret)) {
        return "EMPTY";
      }
      String trimmed = clientSecret.trim();
      return trimmed.substring(0, Math.min(3, trimmed.length())) + "*** (length " + trimmed.length() + ")";
    }

    private static boolean notBlank(String value) {
      return value != null && !value.isBlank();
    }
  }

  public String logoUrl() {
    String base = siteUrl == null || siteUrl.isBlank() ? "https://13.202.214.183/" : siteUrl.trim();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base + "/images/rathin-logo.png";
  }
}

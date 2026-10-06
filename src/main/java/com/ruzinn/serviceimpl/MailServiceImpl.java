package com.ruzinn.serviceimpl;

import com.ruzinn.config.MailProperties;
import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.exception.MailDeliveryException;
import com.ruzinn.service.GraphMailTransport;
import com.ruzinn.service.MailService;
import com.ruzinn.service.MailTransport;
import com.ruzinn.service.SmtpMailTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
  private static final String EMAIL_PATTERN = "(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

  private final MailProperties mailProperties;
  private final SmtpMailTransport smtpTransport;
  private final GraphMailTransport graphTransport;

  public MailServiceImpl(
      MailProperties mailProperties, SmtpMailTransport smtpTransport, GraphMailTransport graphTransport) {
    this.mailProperties = mailProperties;
    this.smtpTransport = smtpTransport;
    this.graphTransport = graphTransport;
  }

  /** Logs the effective mail settings at startup (never secrets) so config problems show up early. */
  @EventListener(ApplicationReadyEvent.class)
  public void logSettings() {
    String provider = mailProperties.effectiveProvider();
    switch (provider) {
      case MailProperties.PROVIDER_GRAPH -> logGraphSettings("Mail provider: Microsoft Graph");
      case MailProperties.PROVIDER_SMTP ->
          log.info(
              "Mail provider: SMTP, host={} port={} username={} password={} from={} starttls={} ssl={}",
              mailProperties.getHost(),
              mailProperties.getPort(),
              mailProperties.getUsername(),
              isBlank(mailProperties.getPassword()) ? "EMPTY" : "set",
              mailProperties.getFrom(),
              mailProperties.isStarttls(),
              mailProperties.isSsl());
      case MailProperties.PROVIDER_LOG ->
          log.warn("Mail provider: none configured; emails will only be logged, not sent");
      default ->
          log.error(
              "Unknown ruzinn.mail.provider '{}' (use auto, smtp, graph or log); emails will fail", provider);
    }
    if (enabled() && isBlank(mailProperties.getFrom())) {
      log.error("ruzinn.mail.from is empty; emails will fail until it is set");
    }
  }

  @Override
  public boolean enabled() {
    return !MailProperties.PROVIDER_LOG.equals(mailProperties.effectiveProvider());
  }

  @Override
  public String transportName() {
    return enabled() ? mailProperties.effectiveProvider() : "local";
  }

  @Override
  public void send(OutgoingMail mail) {
    if (mail.to() == null || !mail.to().trim().matches(EMAIL_PATTERN)) {
      throw new MailDeliveryException("Invalid recipient address: " + mail.to());
    }
    if (!enabled()) {
      log.info("Mail NOT sent (no provider configured): to={} subject=\"{}\"", mail.to(), mail.subject());
      return;
    }
    if (isBlank(mailProperties.getFrom())) {
      throw new MailDeliveryException("Mail sender (ruzinn.mail.from) is not configured");
    }
    String provider = mailProperties.effectiveProvider();
    MailTransport transport = transport(provider);
    if (MailProperties.PROVIDER_GRAPH.equals(provider)) {
      logGraphSettings("Graph settings for this send");
    }
    log.info(
        "Mail send start: provider={} from={} to={} subject=\"{}\"{}",
        provider, mailProperties.getFrom(), mail.to(), mail.subject(),
        mail.hasAttachment() ? " attachment=" + mail.attachmentNameOrDefault() : "");
    long started = System.currentTimeMillis();
    try {
      transport.send(mail);
    } catch (Exception ex) {
      log.error(
          "Mail send FAILED after {} ms: provider={} to={} -> {}",
          System.currentTimeMillis() - started, provider, mail.to(), ex.getMessage(), ex);
      throw ex instanceof MailDeliveryException mde
          ? mde
          : new MailDeliveryException("Could not send email to " + mail.to() + ": " + ex.getMessage(), ex);
    }
    log.info(
        "Mail sent in {} ms: provider={} to={} subject=\"{}\"",
        System.currentTimeMillis() - started, provider, mail.to(), mail.subject());
  }

  private MailTransport transport(String provider) {
    return switch (provider) {
      case MailProperties.PROVIDER_SMTP -> smtpTransport;
      case MailProperties.PROVIDER_GRAPH -> graphTransport;
      default -> throw new MailDeliveryException("Unknown mail provider: " + provider);
    };
  }

  /** Logs the Graph IDs and a masked secret (never the full secret), warning if it is a Secret ID. */
  private void logGraphSettings(String heading) {
    MailProperties.Graph graph = mailProperties.getGraph();
    log.info(
        "{}: from={} tenantId={} clientId={} clientSecret={}",
        heading,
        mailProperties.getFrom(),
        graph.getTenantId(),
        graph.getClientId(),
        graph.maskedClientSecret());
    if (graph.clientSecretLooksLikeSecretId()) {
      log.warn(
          "ruzinn.mail.graph.client-secret is a Secret ID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx), not the secret"
              + " Value; Microsoft will reject it. Create a new client secret and copy its Value column.");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

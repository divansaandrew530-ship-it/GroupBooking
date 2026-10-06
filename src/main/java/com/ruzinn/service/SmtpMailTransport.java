package com.ruzinn.service;

import com.ruzinn.config.MailProperties;
import com.ruzinn.dto.OutgoingMail;
import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.Properties;
import org.springframework.stereotype.Component;

/**
 * SMTP delivery, e.g. Microsoft 365 (smtp.office365.com:587, STARTTLS). Microsoft requires SMTP AUTH
 * to be enabled for the tenant and mailbox, and the From address to be the signed-in mailbox.
 */
@Component
public class SmtpMailTransport implements MailTransport {

  private final MailProperties mailProperties;
  private volatile Session session;

  public SmtpMailTransport(MailProperties mailProperties) {
    this.mailProperties = mailProperties;
  }

  @Override
  public void send(OutgoingMail mail) throws Exception {
    MimeMessage message = new MimeMessage(session());
    message.setFrom(new InternetAddress(mailProperties.getFrom(), mailProperties.getFromName(), "UTF-8"));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.to(), true));
    message.setSubject(mail.subject(), "UTF-8");
    message.setSentDate(new Date());
    MimeMultipart mixed = new MimeMultipart("mixed");
    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(mail.html(), "text/html; charset=UTF-8");
    mixed.addBodyPart(htmlPart);
    if (mail.hasAttachment()) {
      MimeBodyPart filePart = new MimeBodyPart();
      filePart.setFileName(mail.attachmentNameOrDefault());
      filePart.setDataHandler(
          new DataHandler(
              new ByteArrayDataSource(mail.attachment(), mail.attachmentContentTypeOrDefault())));
      mixed.addBodyPart(filePart);
    }
    message.setContent(mixed);
    Transport.send(message);
  }

  private Session session() {
    Session current = session;
    if (current == null) {
      synchronized (this) {
        if (session == null) {
          session = createSession();
        }
        current = session;
      }
    }
    return current;
  }

  private Session createSession() {
    Properties props = new Properties();
    props.put("mail.smtp.host", mailProperties.getHost());
    props.put("mail.smtp.port", String.valueOf(mailProperties.getPort()));
    // Never hang a request thread on a slow or unreachable mail server.
    props.put("mail.smtp.connectiontimeout", String.valueOf(mailProperties.getConnectTimeoutMs()));
    props.put("mail.smtp.timeout", String.valueOf(mailProperties.getReadTimeoutMs()));
    props.put("mail.smtp.writetimeout", String.valueOf(mailProperties.getReadTimeoutMs()));
    boolean auth =
        mailProperties.getUsername() != null && !mailProperties.getUsername().isBlank();
    props.put("mail.smtp.auth", String.valueOf(auth));
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    props.put("mail.smtp.ssl.checkserveridentity", "true");
    if (mailProperties.isSsl()) {
      props.put("mail.smtp.ssl.enable", "true");
    } else if (mailProperties.isStarttls()) {
      props.put("mail.smtp.starttls.enable", "true");
      // Refuse to send credentials over a plain connection if the server does not offer STARTTLS.
      props.put("mail.smtp.starttls.required", "true");
    }
    return Session.getInstance(
        props,
        auth
            ? new Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    mailProperties.getUsername(),
                    mailProperties.getPassword() == null ? "" : mailProperties.getPassword());
              }
            }
            : null);
  }
}

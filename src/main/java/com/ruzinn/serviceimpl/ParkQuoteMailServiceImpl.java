package com.ruzinn.serviceimpl;

import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteDocument;
import com.ruzinn.dto.SentQuoteMail;
import com.ruzinn.service.MailService;
import com.ruzinn.service.MailTemplateRenderer;
import com.ruzinn.service.ParkQuoteMailService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParkQuoteMailServiceImpl implements ParkQuoteMailService {

  private static final Logger log = LoggerFactory.getLogger(ParkQuoteMailServiceImpl.class);

  private final MailTemplateRenderer templates;
  private final MailService mailService;
  private final CopyOnWriteArrayList<SentQuoteMail> sent = new CopyOnWriteArrayList<>();

  public ParkQuoteMailServiceImpl(MailTemplateRenderer templates, MailService mailService) {
    this.templates = templates;
    this.mailService = mailService;
  }

  @Override
  public SentQuoteMail send(String to, QuoteDocument document) {
    return send(to, document, null);
  }

  @Override
  public SentQuoteMail send(String to, QuoteDocument document, ParkQuoteRequest request) {
    if (to == null || !to.matches("(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      throw new IllegalArgumentException("Enter a valid email address");
    }
    String recipient = to.trim();
    String html = templates.renderQuote(document.quote(), request);
    String subject =
        "Rathin quotation "
            + document.quote().getQuoteNumber()
            + " — "
            + document.quote().getCurrency()
            + " "
            + String.format(java.util.Locale.US, "%.2f", document.quote().getTotal());
    String transportName = mailService.transportName();
    try {
      mailService.send(
          OutgoingMail.htmlWithAttachment(
              recipient, subject, html, document.pdf(), document.fileName(), "application/pdf"));
    } catch (Exception ex) {
      log.warn("Quote {} email failed for {}: {}", document.quote().getQuoteNumber(), recipient, ex.getMessage());
      throw new IllegalStateException("Could not send the quote to " + recipient);
    }
    SentQuoteMail mail =
        new SentQuoteMail(
            document.quote().getQuoteNumber(),
            recipient,
            subject,
            html,
            document.pdf(),
            document.fileName(),
            Instant.now(),
            transportName);
    sent.add(mail);
    return mail;
  }

  @Override
  public List<SentQuoteMail> sentMails() {
    return new ArrayList<>(sent);
  }

  @Override
  public void clearSent() {
    sent.clear();
  }
}

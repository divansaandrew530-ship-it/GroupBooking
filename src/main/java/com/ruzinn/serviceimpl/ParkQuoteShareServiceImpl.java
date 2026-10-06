package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkLineRequest;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteDetailView;
import com.ruzinn.dto.QuoteDocument;
import com.ruzinn.dto.QuoteMainView;
import com.ruzinn.dto.SentQuoteMail;
import com.ruzinn.exception.QuoteNotFoundException;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.MailTemplateRenderer;
import com.ruzinn.service.ParkQuoteMailService;
import com.ruzinn.service.ParkQuotePdfService;
import com.ruzinn.service.ParkQuoteService;
import com.ruzinn.service.ParkQuoteShareService;
import com.ruzinn.service.QuoteRecordService;
import com.ruzinn.util.BookingPresentation;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParkQuoteShareServiceImpl implements ParkQuoteShareService {

  private static final Logger log = LoggerFactory.getLogger(ParkQuoteShareServiceImpl.class);

  private static final DateTimeFormatter ISSUED = BookingPresentation.DISPLAY_DATE;

  private final ParkQuoteService quoteService;
  private final ParkQuotePdfService pdfService;
  private final ParkQuoteMailService mailService;
  private final QuoteRecordService quoteRecordService;
  private final MailTemplateRenderer mailTemplates;
  private final AccountService accountService;

  public ParkQuoteShareServiceImpl(
      ParkQuoteService quoteService,
      ParkQuotePdfService pdfService,
      ParkQuoteMailService mailService,
      QuoteRecordService quoteRecordService,
      MailTemplateRenderer mailTemplates,
      AccountService accountService) {
    this.quoteService = quoteService;
    this.pdfService = pdfService;
    this.mailService = mailService;
    this.quoteRecordService = quoteRecordService;
    this.mailTemplates = mailTemplates;
    this.accountService = accountService;
  }

  @Override
  public ParkQuote quote(ParkQuoteRequest request, AuthSession auth) {
    accountService.applyTransactionParty(request, auth);
    applyQuoteRatePolicy(request, auth);
    applyStoredExpireDate(request);
    ParkQuote quote = quoteService.quote(request);
    applyPrepared(quote, request, auth);
    applyBrand(quote, auth);
    QuoteMainView saved = quoteRecordService.saveGenerated(quote, request, auth);
    if (saved != null) {
      quote.setExpireDate(saved.resolvedExpireDate());
    }
    log.info(
        "Quote {} priced: {} item(s), total {} {}, company {}, by {}",
        quote.getQuoteNumber(), quote.getItems() == null ? 0 : quote.getItems().size(), quote.getTotal(),
        quote.getCurrency(), request == null ? "-" : request.getCompanyId(), auth == null ? "guest" : auth.email());
    return quote;
  }

  private void applyStoredExpireDate(ParkQuoteRequest request) {
    if (request == null || request.getExpireDate() != null) {
      return;
    }
    String quoteRef = request.getQuoteNumber();
    if (quoteRef == null || quoteRef.isBlank()) {
      return;
    }
    try {
      QuoteMainView stored = quoteRecordService.get(quoteRef.trim());
      if (stored != null) {
        request.setExpireDate(stored.resolvedExpireDate());
      }
    } catch (QuoteNotFoundException ex) {
      // New quote number — keep the default expire date.
    }
  }

  private void applyQuoteRatePolicy(ParkQuoteRequest request, AuthSession auth) {
    if (request == null) {
      return;
    }
    if (auth == null || !auth.parentCompany()) {
      return;
    }
    clearDirectRateOverrides(request);
    restoreStoredQuoteRates(request);
  }

  private static void clearDirectRateOverrides(ParkQuoteRequest request) {
    if (request.getItems() == null) {
      return;
    }
    for (ParkLineRequest item : request.getItems()) {
      if (item == null) {
        continue;
      }
      item.setAdultRate(null);
      item.setChildRate(null);
    }
  }

  private void restoreStoredQuoteRates(ParkQuoteRequest request) {
    String quoteRef = request.getQuoteNumber();
    if (quoteRef == null || quoteRef.isBlank() || request.getItems() == null) {
      return;
    }
    QuoteMainView stored;
    try {
      stored = quoteRecordService.get(quoteRef.trim());
    } catch (QuoteNotFoundException ex) {
      return;
    }
    if (stored == null || stored.details() == null) {
      return;
    }
    boolean[] used = new boolean[stored.details().size()];
    for (ParkLineRequest item : request.getItems()) {
      if (item == null) {
        continue;
      }
      QuoteDetailView match = takeStoredDetail(stored.details(), used, item);
      if (match == null) {
        continue;
      }
      if (match.adultRate() != null) {
        item.setAdultRate(match.adultRate().doubleValue());
      }
      if (match.childRate() != null) {
        item.setChildRate(match.childRate().doubleValue());
      }
    }
  }

  private static QuoteDetailView takeStoredDetail(
      java.util.List<QuoteDetailView> details, boolean[] used, ParkLineRequest item) {
    for (int i = 0; i < details.size(); i++) {
      if (used[i]) {
        continue;
      }
      QuoteDetailView detail = details.get(i);
      if (!sameDirectLine(detail, item)) {
        continue;
      }
      used[i] = true;
      return detail;
    }
    return null;
  }

  private static boolean sameDirectLine(QuoteDetailView detail, ParkLineRequest item) {
    if (detail == null || item == null) {
      return false;
    }
    String itemId = item.getItemId() == null ? "" : item.getItemId().trim();
    String storedId = detail.itemId() == null ? "" : detail.itemId().trim();
    if (itemId.isEmpty() || !itemId.equalsIgnoreCase(storedId)) {
      return false;
    }
    String visit = item.getVisitDate() == null ? "" : item.getVisitDate().trim();
    String storedVisit = detail.visitDate() == null ? "" : detail.visitDate().toString();
    return visit.equals(storedVisit);
  }

  @Override
  public QuoteDocument document(ParkQuoteRequest request, AuthSession auth) {
    ParkQuote quote = quote(request, auth);
    byte[] pdf = pdfService.render(quote);
    log.info("Quote {} PDF generated ({} bytes)", quote.getQuoteNumber(), pdf == null ? 0 : pdf.length);
    return new QuoteDocument(quote, mailTemplates.renderQuote(quote, request), pdf, fileName(quote));
  }

  @Override
  public SentQuoteMail send(ParkQuoteRequest request, AuthSession auth) {
    String to = request == null ? null : request.getEmail();
    if ((to == null || to.isBlank()) && auth != null) {
      to = auth.email();
    }
    if (to == null || !to.matches("(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      log.warn("Quote email not sent: invalid address '{}'", to);
      throw new IllegalArgumentException("Enter a valid email address");
    }
    QuoteDocument document = document(request, auth);
    SentQuoteMail sent = mailService.send(to, document, request);
    quoteRecordService.markSent(document.quote(), request, auth, sent.to());
    log.info("Quote {} emailed to {} by {}", sent.quoteNumber(), sent.to(), auth == null ? "guest" : auth.email());
    return sent;
  }

  static String fileName(ParkQuote quote) {
    String number = quote.getQuoteNumber() == null ? "quote" : quote.getQuoteNumber();
    return "Rathin-Quote-" + number + ".pdf";
  }

  @Override
  public void applyPrepared(ParkQuote quote, ParkQuoteRequest request, AuthSession auth) {
    if (quote.getIssuedOn() == null || quote.getIssuedOn().isBlank()) {
      quote.setIssuedOn(java.time.LocalDate.now().format(ISSUED));
    }
    if (quote.getExpireDate() == null && request != null && request.getExpireDate() != null) {
      quote.setExpireDate(request.getExpireDate());
    }
    if (quote.getExpireDate() == null) {
      quote.setExpireDate(BookingPresentation.defaultQuoteExpireDate());
    }
    quote.setPreparedBy(
        firstNonBlank(
            request == null ? null : request.getOwnerUserName(),
            auth == null ? null : auth.userName(),
            auth == null ? null : auth.email(),
            "Rathin"));
    String companyName =
        firstNonBlank(
            auth == null ? null : auth.companyName(),
            request == null ? null : request.getCompanyName());
    if (companyName != null && !companyName.isBlank()) {
      quote.setCompanyName(companyName);
    }
  }

  static void applyBrand(ParkQuote quote, com.ruzinn.dto.CompanyBrand brand) {
    if (quote == null || brand == null) {
      return;
    }
    if (quote.getCompanyName() == null || quote.getCompanyName().isBlank()) {
      quote.setCompanyName(brand.displayName());
    }
    quote.setCompanyId(brand.companyId());
    quote.setBrandAddress(brand.address());
    quote.setBrandEmail(brand.email());
    quote.setBrandPhone(brand.phone());
    quote.setBrandWebsite(brand.website());
    quote.setBrandPlace(brand.place());
    quote.setHasLogo(brand.hasLogo());
    quote.setLogoUrl(brand.logoUrl());
  }

  private void applyBrand(ParkQuote quote, AuthSession auth) {
    applyBrand(quote, accountService.brandingFor(auth));
  }

  static String html(ParkQuote quote) {
    StringBuilder rows = new StringBuilder();
    if (quote.getItems() != null) {
      for (ParkQuoteLine line : quote.getItems()) {
        rows.append("<tr>")
            .append("<td><strong>")
            .append(esc(line.getName()))
            .append("</strong><br/><span style=\"color:#64748b;font-size:12px\">")
            .append(esc(line.getKindLabel()))
            .append("</span></td>")
            .append("<td>")
            .append(esc(details(line)).replace("\n", "<br/>"))
            .append("</td>")
            .append("<td>")
            .append(esc(line.getGuestSummary()))
            .append("</td>")
            .append("<td style=\"text-align:right;color:#0057b8;font-weight:700;white-space:nowrap\">")
            .append(esc(money(line.getLineTotal())))
            .append("</td>")
            .append("</tr>");
      }
    }
    String html = """
        <div style="font-family:Arial,Helvetica,sans-serif;color:#1e293b;max-width:680px;margin:0 auto">
          <table width="100%" cellpadding="0" cellspacing="0" style="background:#0057b8;color:#fff;border-radius:12px 12px 0 0">
            <tr>
              <td style="padding:20px 24px">
                <div style="font-size:20px;font-weight:800">Rathin</div>
                <div style="opacity:.85;font-size:13px">GroupBookingLA quotation</div>
              </td>
              <td style="padding:20px 24px;text-align:right">
                <div style="font-size:11px;letter-spacing:.08em;font-weight:700">QUOTATION</div>
                <div style="font-size:18px;font-weight:800">{{number}}</div>
                <div style="opacity:.85;font-size:12px">Issued {{issued}}</div>
              </td>
            </tr>
          </table>
          <div style="border:1px solid #e2e8f0;border-top:0;padding:22px 24px 8px;border-radius:0 0 12px 12px">
            <p style="margin:0 0 4px">Prepared by <strong>{{prepared}}</strong>{{company}}</p>
            <p style="margin:0 0 18px;color:#64748b;font-size:13px">{{validity}}</p>
            <table width="100%" cellpadding="10" cellspacing="0" style="border-collapse:collapse;font-size:13px">
              <thead>
                <tr style="background:#f1f5f9;color:#64748b;text-transform:uppercase;letter-spacing:.04em;font-size:11px">
                  <th align="left">Item</th><th align="left">Details</th><th align="left">Guests</th><th align="right">Amount ({{currency}})</th>
                </tr>
              </thead>
              <tbody>{{rows}}</tbody>
            </table>
            <table width="100%" cellpadding="6" cellspacing="0" style="margin-top:16px;font-size:14px">
              <tr><td>Subtotal</td><td align="right"><strong>{{subtotal}}</strong></td></tr>
              <tr><td>Platform charge</td><td align="right"><strong>{{markup}}</strong></td></tr>
              <tr style="background:#0057b8;color:#fff;font-size:16px">
                <td style="padding:10px 8px">Grand total</td>
                <td align="right" style="padding:10px 8px"><strong>{{total}}</strong></td>
              </tr>
            </table>
          </div>
        </div>
        """;
    return html.replace("{{number}}", esc(quote.getQuoteNumber()))
        .replace("{{issued}}", esc(quote.getIssuedOn()))
        .replace("{{validity}}", esc(quote.getValidityCopy()))
        .replace("{{prepared}}", esc(quote.getPreparedBy()))
        .replace(
            "{{company}}",
            quote.getCompanyName() == null || quote.getCompanyName().isBlank()
                ? ""
                : " · " + esc(quote.getCompanyName()))
        .replace("{{rows}}", rows.toString())
        .replace("{{currency}}", esc(quote.getCurrency() == null || quote.getCurrency().isBlank() ? "AED" : quote.getCurrency()))
        .replace("{{subtotal}}", esc(money(quote.getSubtotal())))
        .replace("{{markup}}", esc(money(quote.getMarkupAmount())))
        .replace("{{total}}", esc(money(quote.getTotal())));
  }

  private static String details(ParkQuoteLine line) {
    if (line.isStay()) {
      StringBuilder details = new StringBuilder(line.getDisplayVisitDate());
      if (line.getCheckOut() != null) {
        details.append(" → ").append(line.getDisplayCheckOut());
      }
      if (line.getNights() != null) {
        details.append("\n").append(line.getNights()).append(line.getNights() == 1 ? " night" : " nights");
      }
      if (line.getRoomType() != null) {
        details.append("\n").append(line.getRoomType());
        if (line.getRooms() > 0) {
          details.append(" · ").append(line.getRooms()).append(line.getRooms() == 1 ? " room" : " rooms");
        }
      }
      return details.toString();
    }
    StringBuilder details = new StringBuilder(line.getDisplayVisitDate());
    if (line.getTimeSlot() != null && !line.getTimeSlot().isBlank()) {
      details.append("\nTimeslot ").append(line.getTimeSlot());
    }
    details.append("\n").append(line.getTicketSummary());
    return details.toString();
  }

  private static String money(double amount) {
    return String.format(Locale.US, "%,.2f", amount);
  }

  private static String esc(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}

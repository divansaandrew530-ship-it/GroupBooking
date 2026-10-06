package com.ruzinn.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.service.MailTemplateRenderer;
import com.ruzinn.service.ParkQuoteMailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MailTemplateRendererTest {

  @Autowired private MailTemplateRenderer renderer;
  @Autowired private ParkQuoteMailService quoteMailService;

  @Test
  void quoteTemplateUsesQuotationCopyAndNumericAmount() {
    ParkQuote quote = sampleQuote();
    ParkQuoteRequest request = new ParkQuoteRequest();
    request.setLeadName("Quote Guest");
    request.setEmail("quote.guest@example.com");
    String html = renderer.renderQuote(quote, request);
    assertThat(html).contains("QUOTATION");
    assertThat(html).contains("QTE-TEST-1");
    assertThat(html).contains("Dubai Frame");
    assertThat(html).contains("Amount (AED)");
    assertThat(html).contains("45.00");
    assertThat(html).doesNotContain("AED 45.00");
    assertThat(html).contains("This is a quotation, not a confirmed booking");
    assertThat(html).contains("Rates are valid until");
    assertThat(html).doesNotContain("Rates are valid for 7 days");
  }

  @Test
  void newBookingMailSaysReceivedAndWaitingForApproval() {
    com.ruzinn.dto.ParkBooking booking = new com.ruzinn.dto.ParkBooking();
    booking.setBookingRef("PRKTEST1234");
    booking.setConfirmationId("CNFTEST1234");
    booking.setLeadName("Ada Guest");
    booking.setEmail("ada@example.com");
    booking.setPhone("+971500000222");
    booking.setQuote(sampleQuote());
    ParkQuoteRequest request = new ParkQuoteRequest();
    request.setLeadName("Ada Guest");
    request.setEmail("ada@example.com");
    String html = renderer.renderBooking(booking, request, "Paid by card");
    assertThat(html).contains("BOOKING RECEIVED").doesNotContain("BOOKING CONFIRMATION");
    assertThat(html).contains("waiting for approval");
    assertThat(html).contains("PRKTEST1234");
    assertThat(html).contains("CNFTEST1234");
    assertThat(html).contains("Ada Guest");
    assertThat(html).contains("Paid by card");
    assertThat(html).contains("Dubai Frame");
  }

  @Test
  void quoteSendWithoutSmtpHostRecordsLocalTransport() {
    quoteMailService.clearSent();
    ParkQuote quote = sampleQuote();
    ParkQuoteRequest request = new ParkQuoteRequest();
    request.setEmail("quote.guest@example.com");
    var document =
        new com.ruzinn.dto.QuoteDocument(
            quote, renderer.renderQuote(quote, request), "%PDF-test".getBytes(), "Rathin-Quote-QTE-TEST-1.pdf");
    var sent = quoteMailService.send("quote.guest@example.com", document, request);
    assertThat(sent.transport()).isEqualTo("local");
    assertThat(sent.html()).contains("QUOTATION");
  }

  private static ParkQuote sampleQuote() {
    ParkQuoteLine line = new ParkQuoteLine();
    line.setItemId("dubai-frame");
    line.setName("Dubai Frame");
    line.setKind("experience");
    line.setVisitDate("2026-09-21");
    line.setAdults(1);
    line.setChildren(0);
    line.setLineTotal(45);
    ParkQuote quote = new ParkQuote();
    quote.setQuoteNumber("QTE-TEST-1");
    quote.setIssuedOn("19-Sep-2026");
    quote.setPreparedBy("Rathin Super Admin");
    quote.setCurrency("AED");
    quote.setSubtotal(45);
    quote.setMarkupAmount(0);
    quote.setTotal(45);
    quote.setItems(java.util.List.of(line));
    return quote;
  }
}

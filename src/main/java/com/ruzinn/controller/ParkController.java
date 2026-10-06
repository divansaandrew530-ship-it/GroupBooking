package com.ruzinn.controller;

import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.BookingPartyCatalog;
import com.ruzinn.dto.ExperienceCatalog;
import com.ruzinn.dto.ParkConfirmRequest;
import com.ruzinn.dto.ParkConfirmationResponse;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.ParkTimeSlotResponse;
import com.ruzinn.dto.QuoteDocument;
import com.ruzinn.dto.SentQuoteMail;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.BookingCheckoutService;
import com.ruzinn.service.ParkBookingService;
import com.ruzinn.service.ParkCatalogService;
import com.ruzinn.service.ParkQuoteService;
import com.ruzinn.service.ParkQuoteShareService;
import com.ruzinn.service.ParkTimeSlotService;
import com.ruzinn.service.RoeService;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Park booking flow: catalog and rates, quotes (save, PDF, email) and checkout.
 *
 * <p>Typical order: {@code GET /catalog} → {@code POST /quote} → {@code POST /confirm}.
 */
@RestController
@RequestMapping("/api/parks")
public class ParkController {

  private final ParkCatalogService catalogService;
  private final ParkQuoteService quoteService;
  private final ParkQuoteShareService shareService;
  private final ParkTimeSlotService timeSlotService;
  private final ParkBookingService bookingService;
  private final BookingCheckoutService checkoutService;
  private final AccountService accountService;
  private final RoeService roeService;

  public ParkController(
      ParkCatalogService catalogService,
      ParkQuoteService quoteService,
      ParkQuoteShareService shareService,
      ParkTimeSlotService timeSlotService,
      ParkBookingService bookingService,
      BookingCheckoutService checkoutService,
      AccountService accountService,
      RoeService roeService) {
    this.catalogService = catalogService;
    this.quoteService = quoteService;
    this.shareService = shareService;
    this.timeSlotService = timeSlotService;
    this.bookingService = bookingService;
    this.checkoutService = checkoutService;
    this.accountService = accountService;
    this.roeService = roeService;
  }

  // ---- catalog ----------------------------------------------------------------------------------

  /** Countries, parks, hotels and their prices. */
  @GetMapping("/catalog")
  public ExperienceCatalog catalog() {
    return catalogService.getCatalog();
  }

  /** Exchange rates from AED (the base currency) to USD and INR. */
  @GetMapping("/roe")
  public Map<String, Object> roe() {
    return Map.of("base", RoeService.AED, "rates", roeService.quoteRates());
  }

  /** Companies and users a parent company may book for (child companies get only themselves). */
  @GetMapping("/booking-party")
  public BookingPartyCatalog bookingParty(HttpSession session) {
    return accountService.bookingParty(SessionUser.current(session));
  }

  /** Time slots for a dated park ({@code date} as yyyy-MM-dd). */
  @GetMapping("/timeslots")
  public ParkTimeSlotResponse timeSlots(@RequestParam String itemId, @RequestParam String date) {
    return timeSlotService.slotsFor(itemId, date);
  }

  // ---- quotes -----------------------------------------------------------------------------------

  /** Prices the itinerary and saves it as a quote; send {@code quoteNumber} to edit an existing one. */
  @PostMapping("/quote")
  public ParkQuote quote(@RequestBody ParkQuoteRequest request, HttpSession session) {
    return shareService.quote(request, SessionUser.current(session));
  }

  @PostMapping(value = "/quote/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> quotePdf(@RequestBody ParkQuoteRequest request, HttpSession session) {
    QuoteDocument document = shareService.document(request, SessionUser.current(session));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(document.pdf());
  }

  /** Emails the quote PDF to {@code email} from the request. */
  @PostMapping("/quote/send")
  public Map<String, Object> sendQuote(@RequestBody ParkQuoteRequest request, HttpSession session) {
    SentQuoteMail sent = shareService.send(request, SessionUser.current(session));
    return Map.of(
        "sent", true,
        "email", sent.to(),
        "quoteNumber", sent.quoteNumber(),
        "fileName", sent.fileName(),
        "message", "Quote " + sent.quoteNumber() + " sent to " + sent.to());
  }

  // ---- bookings ---------------------------------------------------------------------------------

  /**
   * Checkout: validates passengers and payment ({@code paymentMethod} CREDIT or CARD), takes
   * payment and records the booking.
   */
  @PostMapping("/confirm")
  public ParkConfirmationResponse confirm(@RequestBody ParkConfirmRequest request, HttpSession session) {
    return checkoutService.checkout(request, SessionUser.current(session));
  }

  /** Records a booking without payment (status PENDING). */
  @PostMapping("/book")
  public BookingMainView book(@RequestBody ParkQuoteRequest request) {
    return bookingService.confirmAndRecord(quoteService.quote(request), request);
  }

  @GetMapping("/bookings/{bookingRef}")
  public BookingMainView booking(@PathVariable String bookingRef) {
    return bookingService.getRecord(bookingRef);
  }
}

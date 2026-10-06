package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.BookingAccount;
import com.ruzinn.dto.BookingSession;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkConfirmRequest;
import com.ruzinn.dto.ParkConfirmationResponse;
import com.ruzinn.dto.ParkPassenger;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.PaymentIntent;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.BookingCheckoutService;
import com.ruzinn.service.BookingSessionStore;
import com.ruzinn.service.ParkBookingService;
import com.ruzinn.service.ParkQuoteService;
import com.ruzinn.service.PaymentService;
import com.ruzinn.service.QuoteRecordService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingCheckoutServiceImpl implements BookingCheckoutService {

  private static final Logger log = LoggerFactory.getLogger(BookingCheckoutServiceImpl.class);

  private static final String CREDIT = "CREDIT";
  private static final String CARD = "CARD";

  private final AccountService accountService;
  private final ParkBookingService bookingService;
  private final ParkQuoteService quoteService;
  private final PaymentService paymentService;
  private final BookingSessionStore sessions;
  private final QuoteRecordService quoteRecordService;

  public BookingCheckoutServiceImpl(
      AccountService accountService,
      ParkBookingService bookingService,
      ParkQuoteService quoteService,
      PaymentService paymentService,
      BookingSessionStore sessions,
      QuoteRecordService quoteRecordService) {
    this.accountService = accountService;
    this.bookingService = bookingService;
    this.quoteService = quoteService;
    this.paymentService = paymentService;
    this.sessions = sessions;
    this.quoteRecordService = quoteRecordService;
  }

  @Override
  @Transactional
  public ParkConfirmationResponse checkout(ParkConfirmRequest request, AuthSession user) {
    if (request.getLeadName() == null || request.getLeadName().isBlank()) {
      throw new IllegalArgumentException("Lead guest name is required");
    }
    // The booking user comes from the session (or the validated ownerUserId), never from the client.
    request.setUserId(null);
    // Accepting a saved quote: check it can still be booked before any payment is taken.
    String acceptedQuoteRef = null;
    if (request.isAcceptQuote()) {
      acceptedQuoteRef = quoteRecordService.requireAcceptable(user, request.getQuoteNumber()).getQuoteRefNum();
    }
    applyBookingAccount(request, user);

    ParkQuote quote = quoteService.quote(request);
    useLeadContactForParksWithoutPassengerForms(request, quote);
    validatePassengers(quote, request);

    String method = request.getPaymentMethod() == null ? "" : request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
    Payment payment =
        switch (method) {
          case CREDIT -> creditPayment();
          case CARD -> cardPayment(request, quote);
          default -> throw new IllegalArgumentException("Choose company credit or a credit card");
        };

    ParkBooking booking = record(quote, request, user, method);
    if (acceptedQuoteRef != null) {
      // Same transaction as the credit charge and booking: the quote is ACCEPTED only if both succeed.
      quoteRecordService.markAccepted(acceptedQuoteRef, booking.getBookingRef());
    }
    log.info(
        "Booking {} confirmed: company {}, lead {}, {} item(s), total {} {}, paid by {} ({}), by {}",
        booking.getBookingRef(), request.getCompanyId(), request.getLeadName(),
        quote.getItems() == null ? 0 : quote.getItems().size(), quote.getTotal(), quote.getCurrency(),
        method, payment.status(), user == null ? "guest" : user.email());
    return ParkConfirmationResponse.from(
        booking, quote, method, payment.status(), payment.transactionId(), payment.cardLast4());
  }

  @Override
  @Transactional
  public ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String paymentMethod) {
    return record(quote, request, auth, paymentMethod);
  }

  /**
   * Charges company credit when paid that way, then saves the booking. The credit is taken from the
   * booked company (a parent booking for a child company uses the child's credit).
   */
  private ParkBooking record(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String paymentMethod) {
    if (CREDIT.equals(paymentMethod)) {
      accountService.chargeCredit(
          auth, request.getCompanyId(), BigDecimal.valueOf(quote.creditTotal()).setScale(2, RoundingMode.HALF_UP));
    }
    return bookingService.confirm(quote, request, paymentNote(paymentMethod));
  }

  // ---- booking account --------------------------------------------------------------------------

  /** Signed-in users book for their company (or the selected child company); guests by email. */
  private void applyBookingAccount(ParkQuoteRequest request, AuthSession user) {
    if (user != null) {
      accountService.applyTransactionParty(request, user);
      return;
    }
    BookingAccount account = accountService.resolveBookingAccount(null, request.getEmail());
    request.setUserId(account.username());
    request.setCompanyName(account.companyName());
    request.setCompanyId(account.companyId());
  }

  // ---- passengers -------------------------------------------------------------------------------

  /** Parks that need no passenger form are booked under the lead guest's contact details. */
  private static void useLeadContactForParksWithoutPassengerForms(ParkQuoteRequest request, ParkQuote quote) {
    if (request.getItems() == null || quote.getItems() == null) {
      return;
    }
    for (int i = 0; i < quote.getItems().size(); i++) {
      ParkQuoteLine line = quote.getItems().get(i);
      if (!line.isPark() || line.isRequiresPassengerDetails()) {
        continue;
      }
      ParkPassenger lead = leadPassenger(request);
      line.setPassengers(List.of(lead));
      if (i < request.getItems().size()) {
        request.getItems().get(i).setPassengers(List.of(lead));
      }
    }
  }

  private static ParkPassenger leadPassenger(ParkQuoteRequest request) {
    String name = request.getLeadName() == null ? "" : request.getLeadName().trim();
    int space = name.indexOf(' ');
    ParkPassenger lead = new ParkPassenger();
    lead.setType("ADULT");
    lead.setFirstName(space > 0 ? name.substring(0, space).trim() : name);
    if (space > 0) {
      lead.setLastName(name.substring(space + 1).trim());
    }
    lead.setEmail(request.getEmail());
    lead.setPhone(request.getPhone());
    return lead;
  }

  /** Parks that require passenger details need one filled-in passenger per adult and child. */
  private static void validatePassengers(ParkQuote quote, ParkQuoteRequest request) {
    if (quote.getItems() == null) {
      return;
    }
    for (int i = 0; i < quote.getItems().size(); i++) {
      ParkQuoteLine line = quote.getItems().get(i);
      if (!line.isRequiresPassengerDetails()) {
        continue;
      }
      List<ParkPassenger> passengers = i < request.getItems().size() ? request.getItems().get(i).getPassengers() : List.of();
      if (passengers == null || passengers.size() != line.getAdults() + line.getChildren()) {
        throw new IllegalArgumentException("Enter passenger details for " + line.getName());
      }
      for (ParkPassenger passenger : passengers) {
        validatePassenger(line, passenger);
      }
    }
  }

  private static void validatePassenger(ParkQuoteLine line, ParkPassenger passenger) {
    if (line.getPassengerFields() == null || line.getPassengerFields().isEmpty()) {
      if (isBlank(passenger.getFirstName()) || isBlank(passenger.getLastName())) {
        throw new IllegalArgumentException("Passenger names are required for " + line.getName());
      }
      return;
    }
    for (var field : line.getPassengerFields()) {
      if (isBlank(passenger.valueFor(field.getKey()))) {
        throw new IllegalArgumentException(field.getLabel() + " is required for " + line.getName());
      }
    }
  }

  // ---- payment ----------------------------------------------------------------------------------

  private record Payment(String status, String transactionId, String cardLast4) {}

  /** Company credit is charged when the booking is recorded (see {@link #record}). */
  private static Payment creditPayment() {
    return new Payment("success", "CRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT), null);
  }

  /** Validates the card and runs the (mock) card payment. */
  private Payment cardPayment(ParkConfirmRequest request, ParkQuote quote) {
    validateCard(request.getCardName(), request.getCardNumber(), request.getCardExpiry(), request.getCardCvc());
    BookingSession paymentSession = sessions.create("parks-api");
    PaymentIntent intent =
        paymentService.create(
            paymentSession.getId(), quote.getTotal(), quote.getCurrency(), "parks", paymentSession.getId(), "/api/parks/confirm");
    Map<String, Object> result = paymentService.confirm(paymentSession.getId(), intent.getPaymentId(), "success");
    return new Payment(String.valueOf(result.get("status")), (String) result.get("transactionId"), last4(request.getCardNumber()));
  }

  private static void validateCard(String name, String number, String expiry, String cvc) {
    if (isBlank(name)) {
      throw new IllegalArgumentException("Cardholder name is required");
    }
    if (!digitsOnly(number).matches("\\d{13,19}")) {
      throw new IllegalArgumentException("Enter a valid card number");
    }
    if (expiry == null || !expiry.trim().matches("\\d{2}/\\d{2}")) {
      throw new IllegalArgumentException("Enter card expiry as MM/YY");
    }
    if (cvc == null || !cvc.trim().matches("\\d{3,4}")) {
      throw new IllegalArgumentException("Enter a valid CVC");
    }
  }

  private static String last4(String number) {
    String digits = digitsOnly(number);
    return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
  }

  private static String paymentNote(String paymentMethod) {
    if (CREDIT.equals(paymentMethod)) {
      return "Paid with company credit";
    }
    if (CARD.equals(paymentMethod)) {
      return "Paid by card";
    }
    return "Booking confirmed";
  }

  private static String digitsOnly(String value) {
    return value == null ? "" : value.replaceAll("\\s", "");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

package com.ruzinn.serviceimpl;

import com.ruzinn.dto.BookingSession;
import com.ruzinn.dto.PaymentIntent;
import com.ruzinn.service.BookingSessionStore;
import com.ruzinn.service.PaymentService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

  private final BookingSessionStore sessions;

  public PaymentServiceImpl(BookingSessionStore sessions) {
    this.sessions = sessions;
  }

  @Override
  public PaymentIntent create(
      String sessionId, double amount, String currency, String module, String bookingRef, String returnPath) {
    BookingSession session = sessions.require(sessionId);
    PaymentIntent intent = new PaymentIntent();
    intent.setPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    intent.setAmount(amount);
    intent.setCurrency(currency == null ? "INR" : currency);
    intent.setModule(module);
    intent.setBookingRef(bookingRef);
    intent.setStatus("pending");
    intent.setRedirectUrl(returnPath + "?paymentId=" + intent.getPaymentId() + "&sessionId=" + sessionId);
    intent.setCreatedAt(Instant.now().toString());
    session.put("payment", intent);
    log.info(
        "Payment {} created: {} {} for {} {} (session {})",
        intent.getPaymentId(), amount, intent.getCurrency(), module, bookingRef, sessionId);
    return intent;
  }

  @Override
  public Map<String, Object> confirm(String sessionId, String paymentId, String outcome) {
    BookingSession session = sessions.require(sessionId);
    PaymentIntent payment = session.get("payment", PaymentIntent.class);
    Map<String, Object> confirmation = new HashMap<>();
    confirmation.put("paymentId", paymentId);
    if (payment == null || !paymentId.equals(payment.getPaymentId())) {
      log.warn("Payment {} failed: not found in session {}", paymentId, sessionId);
      confirmation.put("status", "failed");
      confirmation.put("failureReason", "Payment session not found");
      return confirmation;
    }
    if ("success".equalsIgnoreCase(outcome)) {
      confirmation.put("status", "success");
      confirmation.put("transactionId", ("TXN" + UUID.randomUUID().toString().replace("-", "")).substring(0, 14).toUpperCase());
      confirmation.put("paidAt", Instant.now().toString());
      payment.setStatus("success");
    } else {
      confirmation.put("status", "failed");
      confirmation.put("failureReason", "Payment declined by gateway");
      payment.setStatus("failed");
    }
    session.put("payment", payment);
    session.put("paymentConfirmation", confirmation);
    log.info(
        "Payment {} {} (transaction {})",
        paymentId, confirmation.get("status"), confirmation.getOrDefault("transactionId", "-"));
    return confirmation;
  }
}

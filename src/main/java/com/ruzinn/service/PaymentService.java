package com.ruzinn.service;

import com.ruzinn.dto.PaymentIntent;
import java.util.Map;

public interface PaymentService {

  PaymentIntent create(
      String sessionId, double amount, String currency, String module, String bookingRef, String returnPath);

  Map<String, Object> confirm(String sessionId, String paymentId, String outcome);
}

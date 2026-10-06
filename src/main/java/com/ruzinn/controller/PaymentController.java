package com.ruzinn.controller;

import com.ruzinn.dto.PaymentIntent;
import com.ruzinn.service.PaymentService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/create")
  public Map<String, Object> create(@RequestBody Map<String, Object> body) {
    PaymentIntent intent =
        paymentService.create(
            String.valueOf(body.get("sessionId")),
            ((Number) body.get("amount")).doubleValue(),
            String.valueOf(body.getOrDefault("currency", "INR")),
            String.valueOf(body.get("module")),
            String.valueOf(body.get("bookingRef")),
            String.valueOf(body.getOrDefault("returnPath", "/flights/confirmation")));
    return Map.of("payment", intent);
  }

  @PostMapping("/confirm")
  public Map<String, Object> confirm(@RequestBody Map<String, Object> body) {
    Map<String, Object> confirmation =
        paymentService.confirm(
            String.valueOf(body.get("sessionId")),
            String.valueOf(body.get("paymentId")),
            String.valueOf(body.getOrDefault("outcome", "success")));
    return Map.of("confirmation", confirmation);
  }
}

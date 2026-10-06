package com.ruzinn.dto;

public class PaymentIntent {
  private String paymentId;
  private double amount;
  private String currency;
  private String module;
  private String bookingRef;
  private String status;
  private String redirectUrl;
  private String createdAt;

  public String getPaymentId() { return paymentId; }
  public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
  public double getAmount() { return amount; }
  public void setAmount(double amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getModule() { return module; }
  public void setModule(String module) { this.module = module; }
  public String getBookingRef() { return bookingRef; }
  public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRedirectUrl() { return redirectUrl; }
  public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

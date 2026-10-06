package com.ruzinn.dto;

public class ParkBooking {
  private String bookingRef;
  private String confirmationId;
  private String status;
  private String leadName;
  private String email;
  private String phone;
  private ParkQuote quote;

  public String getBookingRef() {
    return bookingRef;
  }

  public void setBookingRef(String bookingRef) {
    this.bookingRef = bookingRef;
  }

  public String getConfirmationId() {
    return confirmationId;
  }

  public void setConfirmationId(String confirmationId) {
    this.confirmationId = confirmationId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLeadName() {
    return leadName;
  }

  public void setLeadName(String leadName) {
    this.leadName = leadName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public ParkQuote getQuote() {
    return quote;
  }

  public void setQuote(ParkQuote quote) {
    this.quote = quote;
  }
}

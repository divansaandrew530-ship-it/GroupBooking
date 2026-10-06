package com.ruzinn.dto;

/**
 * Response of {@code POST /api/parks/confirm}: the booking, the priced itinerary and the payment
 * outcome, everything the confirmation screen needs in one payload.
 */
public record ParkConfirmationResponse(
    String bookingRef,
    String confirmationId,
    String status,
    String leadName,
    String email,
    String phone,
    double total,
    String currency,
    String paymentMethod,
    String paymentStatus,
    String transactionId,
    String cardLast4,
    ParkQuote quote,
    SelectedItinerary itinerary) {

  public static ParkConfirmationResponse from(
      ParkBooking booking,
      ParkQuote quote,
      String paymentMethod,
      String paymentStatus,
      String transactionId,
      String cardLast4) {
    return new ParkConfirmationResponse(
        booking.getBookingRef(),
        booking.getConfirmationId(),
        booking.getStatus(),
        booking.getLeadName(),
        booking.getEmail(),
        booking.getPhone(),
        quote.getTotal(),
        quote.getCurrency(),
        paymentMethod,
        paymentStatus,
        transactionId,
        cardLast4,
        quote,
        SelectedItinerary.from(quote));
  }
}

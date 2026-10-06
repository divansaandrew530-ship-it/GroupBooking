package com.ruzinn.dto;

import java.util.List;

/**
 * Data for the booking email. {@code confirmed} is false for a new booking (received, waiting for
 * approval) and true once the booking's status is CONFIRMED.
 */
public record BookingMailModel(
    String logoUrl,
    String bookingRef,
    String confirmationId,
    String bookedOn,
    String guestName,
    String guestEmail,
    String guestPhone,
    String companyName,
    String paymentNote,
    String currency,
    List<MailLineView> lines,
    String total,
    boolean confirmed) {}

package com.ruzinn.dto;

import java.time.Instant;

public record SentBookingMail(
    String bookingRef,
    String confirmationId,
    String to,
    String subject,
    String html,
    Instant sentAt,
    String transport) {}

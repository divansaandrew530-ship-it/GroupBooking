package com.ruzinn.util;

import java.util.List;
import java.util.Locale;

public final class QuotePresentation {

  public static final String DRAFT = "DRAFT";
  public static final String SENT = "SENT";
  public static final String PENDING = "PENDING";
  public static final String ACCEPTED = "ACCEPTED";
  public static final String DECLINED = "DECLINED";
  public static final String EXPIRED = "EXPIRED";
  public static final String BOOKED = "BOOKED";

  public static final List<String> EDITABLE_STATUSES =
      List.of(DRAFT, SENT, PENDING, ACCEPTED, DECLINED, EXPIRED, BOOKED);

  private QuotePresentation() {}

  public static String normalize(String status) {
    if (status == null || status.isBlank()) {
      return DRAFT;
    }
    String value = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    return switch (value) {
      case "DRAFT", "NEW", "CREATED" -> DRAFT;
      case "SENT", "EMAILED" -> SENT;
      case "PENDING", "AWAITING", "FOLLOW_UP" -> PENDING;
      case "ACCEPTED", "ACCEPT", "WON" -> ACCEPTED;
      case "DECLINED", "DECLINE", "REJECTED", "LOST" -> DECLINED;
      case "EXPIRED", "EXPIRE" -> EXPIRED;
      case "BOOKED", "CONFIRMED", "CONVERTED" -> BOOKED;
      default -> value;
    };
  }

  public static boolean isEditable(String status) {
    return EDITABLE_STATUSES.contains(normalize(status));
  }

  public static String label(String status) {
    return switch (normalize(status)) {
      case SENT -> "Sent";
      case PENDING -> "Pending";
      case ACCEPTED -> "Accepted";
      case DECLINED -> "Declined";
      case EXPIRED -> "Expired";
      case BOOKED -> "Booked";
      default -> "Draft";
    };
  }

  public static String tone(String status) {
    return switch (normalize(status)) {
      case SENT -> "sent";
      case PENDING -> "pending";
      case ACCEPTED -> "accepted";
      case DECLINED -> "declined";
      case EXPIRED -> "expired";
      case BOOKED -> "booked";
      default -> "draft";
    };
  }
}

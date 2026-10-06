package com.ruzinn.util;

import com.ruzinn.entity.BookingDetail;
import com.ruzinn.entity.BookingMain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BookingPresentation {

  public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Dubai");
  public static final String CONFIRMED = "CONFIRMED";
  public static final String PENDING = "PENDING";
  public static final String REQUESTED = "REQUESTED";
  public static final String COMPLETED = "COMPLETED";
  public static final String CANCELLED = "CANCELLED";
  public static final String REJECTED = "REJECTED";
  public static final List<String> EDITABLE_STATUSES =
      List.of(CONFIRMED, PENDING, REQUESTED, COMPLETED, CANCELLED, REJECTED);
  public static final List<String> POPUP_STATUSES =
      List.of(PENDING, CONFIRMED, CANCELLED, COMPLETED);

  public static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter CREATED =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy, HH:mm", Locale.ENGLISH);
  private static final DateTimeFormatter MONTH_YEAR =
      DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

  private BookingPresentation() {}

  public static String currentMonthLabel() {
    return MONTH_YEAR.format(LocalDate.now(DISPLAY_ZONE));
  }

  public static String rollupStatus(List<String> itemStatuses) {
    if (itemStatuses == null || itemStatuses.isEmpty()) {
      return CONFIRMED;
    }
    List<String> normalized = itemStatuses.stream().map(BookingPresentation::normalize).toList();
    if (normalized.stream().allMatch(CANCELLED::equals)) {
      return CANCELLED;
    }
    if (normalized.stream().anyMatch(PENDING::equals)) {
      return PENDING;
    }
    if (normalized.stream().anyMatch(REQUESTED::equals)) {
      return REQUESTED;
    }
    List<String> active =
        normalized.stream().filter(status -> !CANCELLED.equals(status) && !REJECTED.equals(status)).toList();
    if (normalized.stream().anyMatch(REJECTED::equals) && active.isEmpty()) {
      return REJECTED;
    }
    if (!active.isEmpty() && active.stream().allMatch(COMPLETED::equals)) {
      return COMPLETED;
    }
    if (normalized.stream().anyMatch(CONFIRMED::equals)) {
      return CONFIRMED;
    }
    if (normalized.stream().anyMatch(COMPLETED::equals)) {
      return COMPLETED;
    }
    if (normalized.stream().anyMatch(REJECTED::equals)) {
      return REJECTED;
    }
    return CONFIRMED;
  }

  public static String deriveStatus(BookingMain main) {
    LocalDate today = LocalDate.now(DISPLAY_ZONE);
    List<LocalDate> dates = new ArrayList<>();
    if (main != null && main.getDetails() != null) {
      for (BookingDetail detail : main.getDetails()) {
        if (detail.getShowDate() != null) {
          dates.add(detail.getShowDate());
        }
        if (detail.getCheckoutDate() != null) {
          dates.add(detail.getCheckoutDate());
        } else if (detail.getCheckinDate() != null) {
          dates.add(detail.getCheckinDate());
        }
      }
    }
    if (!dates.isEmpty() && dates.stream().allMatch(date -> date.isBefore(today))) {
      return COMPLETED;
    }
    return CONFIRMED;
  }

  public static String normalize(String status) {
    if (status == null || status.isBlank()) {
      return CONFIRMED;
    }
    String value = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    if ("CANCELED".equals(value) || "CANCEL".equals(value)) {
      return CANCELLED;
    }
    if ("CONFIRM".equals(value) || "CONFIRMED".equals(value)) {
      return CONFIRMED;
    }
    if ("COMPLETE".equals(value) || "COMPLETED".equals(value)) {
      return COMPLETED;
    }
    if ("PENDING".equals(value) || "AWAITING".equals(value)) {
      return PENDING;
    }
    if ("REQUESTED".equals(value) || "REQUEST".equals(value) || "ON_REQUEST".equals(value)) {
      return REQUESTED;
    }
    if ("REJECTED".equals(value) || "REJECT".equals(value)) {
      return REJECTED;
    }
    if ("CANCELLED".equals(value)) {
      return CANCELLED;
    }
    return value;
  }

  public static boolean isEditable(String status) {
    return EDITABLE_STATUSES.contains(normalize(status));
  }

  public static String label(String status) {
    return switch (normalize(status)) {
      case PENDING -> "Pending";
      case REQUESTED -> "Requested";
      case COMPLETED -> "Completed";
      case CANCELLED -> "Cancelled";
      case REJECTED -> "Rejected";
      default -> "Confirmed";
    };
  }

  public static String tone(String status) {
    return switch (normalize(status)) {
      case PENDING -> "pending";
      case REQUESTED -> "requested";
      case COMPLETED -> "completed";
      case CANCELLED -> "cancelled";
      case REJECTED -> "rejected";
      default -> "confirmed";
    };
  }

  public static String formatTravel(java.time.LocalDate date) {
    if (date == null) {
      return "";
    }
    return DISPLAY_DATE.format(date);
  }

  public static LocalDate defaultQuoteExpireDate() {
    return LocalDate.now(DISPLAY_ZONE).plusDays(7);
  }

  public static LocalDate quoteExpireDate(LocalDate expireDate, Instant createdDate) {
    if (expireDate != null) {
      return expireDate;
    }
    if (createdDate != null) {
      return createdDate.atZone(DISPLAY_ZONE).toLocalDate().plusDays(7);
    }
    return defaultQuoteExpireDate();
  }

  public static String quoteValidityCopy(LocalDate expireDate) {
    String until = formatTravel(quoteExpireDate(expireDate, null));
    return "This is a quotation, not a confirmed booking. Rates are valid until " + until + ".";
  }

  public static String formatCreated(Instant createdDate) {
    if (createdDate == null) {
      return "";
    }
    return CREATED.format(createdDate.atZone(DISPLAY_ZONE));
  }

  public static String formatAmount(BigDecimal value) {
    return formatAmount(value, "AED");
  }

  public static String formatAmount(BigDecimal value, String currency) {
    BigDecimal amount = value == null ? BigDecimal.ZERO : value;
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
    DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
    format.setRoundingMode(RoundingMode.HALF_UP);
    return normalizeCurrency(currency) + " " + format.format(amount);
  }

  public static String formatRoe(String currency, BigDecimal rate) {
    String code = normalizeCurrency(currency);
    BigDecimal amount = rate == null ? BigDecimal.ONE : rate;
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
    DecimalFormat format = new DecimalFormat("#,##0.######", symbols);
    format.setRoundingMode(RoundingMode.HALF_UP);
    return "1 AED = " + format.format(amount) + " " + code;
  }

  public static String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return "AED";
    }
    return currency.trim().toUpperCase(Locale.ROOT);
  }

  public static BigDecimal displayAmount(BigDecimal value, BigDecimal convertedValue) {
    return convertedValue != null ? convertedValue : value;
  }

  public static String documentKind(String inventoryType) {
    String type = inventoryType == null ? "" : inventoryType.trim().toUpperCase(Locale.ROOT);
    if ("HOTEL".equals(type)) {
      return "voucher";
    }
    if ("VISA".equals(type)) {
      return "visa";
    }
    return "ticket";
  }

  public static String documentLabel(String kind) {
    return switch (normalizeDocumentKind(kind)) {
      case "voucher" -> "Voucher";
      case "visa" -> "Visa copy";
      default -> "Ticket";
    };
  }

  public static String normalizeDocumentKind(String kind) {
    String value = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    if ("voucher".equals(value)) {
      return "voucher";
    }
    if ("ticket".equals(value)) {
      return "ticket";
    }
    if ("visa".equals(value) || "visa_copy".equals(value) || "visacopy".equals(value)) {
      return "visa";
    }
    throw new IllegalArgumentException("Document must be a voucher, ticket, or visa copy");
  }

  public static boolean storesAsTicket(String kind) {
    return "ticket".equals(normalizeDocumentKind(kind));
  }
}

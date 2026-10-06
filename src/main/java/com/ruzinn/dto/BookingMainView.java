package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public record BookingMainView(
        String bookingRefNum,
        String userName,
        String userId,
        String companyName,
        String companyId,
        BigDecimal value,
        String currency,
        BigDecimal roeRate,
        BigDecimal convertedValue,
        Instant createdDate,
        String status,
        String leadEmail,
        String leadPhone,
        String voucherFileName,
        String ticketFileName,
        List<BookingDetailView> details,
        BigDecimal actualValue,
        BigDecimal actualConvertedValue,
        boolean createdByChild,
        boolean hideQuotedRate,
        boolean showQuotedRate) {

  public int itemCount() {
    return details == null ? 0 : details.size();
  }

  public int passengerCount() {
    if (details == null) {
      return 0;
    }
    int total = 0;
    for (BookingDetailView detail : details) {
      total += detail.adultCount() + detail.childCount() + detail.infantCount() + detail.toddlerCount();
    }
    return total;
  }

  public String statusLabel() {
    return BookingPresentation.label(status);
  }

  public String statusTone() {
    return BookingPresentation.tone(status);
  }

  public String createdDateLabel() {
    return BookingPresentation.formatCreated(createdDate);
  }

  public String totalAmountLabel() {
    return BookingPresentation.formatAmount(displayValue(), currency);
  }

  public String quotedAmountLabel() {
    return BookingPresentation.formatAmount(BookingPresentation.displayAmount(value, convertedValue), currency);
  }

  public String actualAmountLabel() {
    return BookingPresentation.formatAmount(
        BookingPresentation.displayAmount(
            actualValue != null ? actualValue : value,
            actualConvertedValue != null ? actualConvertedValue : convertedValue),
        currency);
  }

  public String rateKindLabel() {
    if (hideQuotedRate) {
      return "Actual rate";
    }
    if (showQuotedRate) {
      return "Quoted Rate";
    }
    return "";
  }

  public BookingMainView forViewer(boolean parentCompany) {
    boolean hide = parentCompany && createdByChild;
    boolean showQuoted = !parentCompany && createdByChild;
    List<BookingDetailView> next =
        details == null
            ? List.of()
            : details.stream().map(detail -> detail.forViewer(parentCompany, createdByChild)).toList();
    return new BookingMainView(
        bookingRefNum,
        userName,
        userId,
        companyName,
        companyId,
        hide && actualValue != null ? actualValue : value,
        currency,
        roeRate,
        hide && actualConvertedValue != null ? actualConvertedValue : convertedValue,
        createdDate,
        status,
        leadEmail,
        leadPhone,
        voucherFileName,
        ticketFileName,
        next,
        actualValue,
        actualConvertedValue,
        createdByChild,
        hide,
        showQuoted);
  }

  public String currencyLabel() {
    return BookingPresentation.normalizeCurrency(currency);
  }

  public String roeRateLabel() {
    return BookingPresentation.formatRoe(currency, roeRate);
  }

  public String convertedAmountLabel() {
    return BookingPresentation.formatAmount(displayValue(), currency);
  }

  public BigDecimal displayValue() {
    if (hideQuotedRate) {
      return BookingPresentation.displayAmount(
          actualValue != null ? actualValue : value,
          actualConvertedValue != null ? actualConvertedValue : convertedValue);
    }
    return BookingPresentation.displayAmount(value, convertedValue);
  }

  public String leadEmailLabel() {
    return leadEmail == null ? "" : leadEmail;
  }

  public String leadPhoneLabel() {
    return leadPhone == null ? "" : leadPhone;
  }

  public boolean hasVoucher() {
    if (voucherFileName != null && !voucherFileName.isBlank()) {
      return true;
    }
    if (details == null) {
      return false;
    }
    return details.stream().anyMatch(BookingDetailView::hasVoucher);
  }

  public boolean hasTicket() {
    if (ticketFileName != null && !ticketFileName.isBlank()) {
      return true;
    }
    if (details == null) {
      return false;
    }
    return details.stream().anyMatch(BookingDetailView::hasTicket);
  }

  public Long primaryItemId() {
    if (details == null || details.isEmpty()) {
      return null;
    }
    return details.get(0).id();
  }

  public String primaryItemName() {
    if (details == null || details.isEmpty() || details.get(0).itemName() == null) {
      return "Booking";
    }
    return details.get(0).itemName();
  }

  public String createdStampLabel() {
    String created = createdDateLabel();
    return created.isBlank() ? "" : "Created " + created;
  }

  public String itemSummaryLabel() {
    int items = itemCount();
    int guests = passengerCount();
    String itemWord = items == 1 ? "item" : "items";
    String guestWord = guests == 1 ? "passenger" : "passengers";
    return items + " " + itemWord + " · " + guests + " " + guestWord;
  }

  public String listAction() {
    return switch (statusTone()) {
      case "confirmed" -> "download";
      case "pending", "requested", "rejected" -> "edit";
      case "completed" -> "upload";
      default -> "none";
    };
  }

  public String travelDateLabel() {
    if (details == null) {
      return "";
    }
    for (BookingDetailView detail : details) {
      LocalDate date = detail.showDate() != null ? detail.showDate() : detail.checkinDate();
      if (date != null) {
        return BookingPresentation.formatTravel(date);
      }
    }
    return "";
  }

  public String searchText() {
    StringJoiner joiner = new StringJoiner(" ");
    if (bookingRefNum != null) {
      joiner.add(bookingRefNum);
    }
    if (userName != null) {
      joiner.add(userName);
    }
    if (leadEmail != null) {
      joiner.add(leadEmail);
    }
    if (details != null) {
      for (BookingDetailView detail : details) {
        if (detail.itemName() != null) {
          joiner.add(detail.itemName());
        }
        if (detail.serviceName() != null) {
          joiner.add(detail.serviceName());
        }
      }
    }
    return joiner.toString().toLowerCase(Locale.ROOT);
  }
}

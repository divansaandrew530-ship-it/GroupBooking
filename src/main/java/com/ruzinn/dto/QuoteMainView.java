package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;
import com.ruzinn.util.QuotePresentation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public record QuoteMainView(
    String quoteRefNum,
    String userName,
    String userId,
    String companyName,
    String companyId,
    BigDecimal value,
    String currency,
    Instant createdDate,
    Instant updatedDate,
    String status,
    String customerName,
    String customerEmail,
    String customerPhone,
    String followUp,
    String sentTo,
    Instant sentAt,
    BigDecimal markupPercent,
    BigDecimal subtotal,
    BigDecimal markupAmount,
    BigDecimal roeRate,
    BigDecimal convertedValue,
    String bookingRefNum,
    boolean canEditItems,
    List<QuoteDetailView> details,
    String packageInclusions,
    String packageExclusions,
    String guestInformation,
    List<ParkDayPlan> dayPlans,
    boolean directCustomer,
    java.time.LocalDate expireDate) {

  public QuoteMainView {
    packageInclusions = packageInclusions == null ? "" : packageInclusions;
    packageExclusions = packageExclusions == null ? "" : packageExclusions;
    guestInformation = guestInformation == null ? "" : guestInformation;
    dayPlans = dayPlans == null ? List.of() : List.copyOf(dayPlans);
  }

  public int itemCount() {
    return details == null ? 0 : details.size();
  }

  public String statusLabel() {
    return QuotePresentation.label(status);
  }

  public String statusTone() {
    return QuotePresentation.tone(status);
  }

  public String createdDateLabel() {
    return BookingPresentation.formatCreated(createdDate);
  }

  public String updatedDateLabel() {
    return BookingPresentation.formatCreated(updatedDate);
  }

  public String sentAtLabel() {
    return BookingPresentation.formatCreated(sentAt);
  }

  public String totalAmountLabel() {
    return BookingPresentation.formatAmount(displayValue(), currency);
  }

  public String subtotalAmountLabel() {
    return BookingPresentation.formatAmount(subtotal, currency);
  }

  public String markupAmountLabel() {
    return BookingPresentation.formatAmount(markupAmount, currency);
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

  public BigDecimal actualTotal() {
    if (details == null || details.isEmpty()) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    boolean any = false;
    for (QuoteDetailView detail : details) {
      BigDecimal part =
          detail.actualConvertedRate() != null ? detail.actualConvertedRate() : detail.actualLineTotal();
      if (part != null) {
        sum = sum.add(part);
        any = true;
      }
    }
    return any ? sum : null;
  }

  public boolean hasActualTotal() {
    return actualTotal() != null;
  }

  public String actualTotalAmountLabel() {
    return BookingPresentation.formatAmount(actualTotal(), currency);
  }

  public BigDecimal displayValue() {
    return BookingPresentation.displayAmount(value, convertedValue);
  }

  public String followUpLabel() {
    return followUp == null ? "" : followUp;
  }

  public String sentToLabel() {
    return sentTo == null ? "" : sentTo;
  }

  public String customerNameLabel() {
    return customerName == null ? "" : customerName;
  }

  public String customerEmailLabel() {
    return customerEmail == null ? "" : customerEmail;
  }

  public String customerPhoneLabel() {
    return customerPhone == null ? "" : customerPhone;
  }

  public boolean hasCustomer() {
    return !customerNameLabel().isBlank() || !customerEmailLabel().isBlank() || !customerPhoneLabel().isBlank();
  }

  public boolean ownedBy(String viewerCompanyId) {
    if (viewerCompanyId == null || companyId == null) {
      return false;
    }
    return viewerCompanyId.trim().equals(companyId.trim());
  }

  public QuoteMainView forViewer(String viewerCompanyId) {
    if (ownedBy(viewerCompanyId) || !hasCustomer()) {
      return this;
    }
    return new QuoteMainView(
        quoteRefNum,
        userName,
        userId,
        companyName,
        companyId,
        value,
        currency,
        createdDate,
        updatedDate,
        status,
        null,
        null,
        null,
        followUp,
        sentTo,
        sentAt,
        markupPercent,
        subtotal,
        markupAmount,
        roeRate,
        convertedValue,
        bookingRefNum,
        canEditItems,
        details,
        packageInclusions,
        packageExclusions,
        guestInformation,
        dayPlans,
        directCustomer,
        expireDate);
  }

  public String companyLabel() {
    return companyName == null ? "" : companyName;
  }

  public String userLabel() {
    if (userName != null && !userName.isBlank()) {
      return userName;
    }
    return userId == null ? "" : userId;
  }

  public String createdStampLabel() {
    String created = createdDateLabel();
    return created.isBlank() ? "" : "Created " + created;
  }

  public java.time.LocalDate resolvedExpireDate() {
    return BookingPresentation.quoteExpireDate(expireDate, createdDate);
  }

  public String expireDateLabel() {
    return BookingPresentation.formatTravel(resolvedExpireDate());
  }

  public String expireDateIso() {
    java.time.LocalDate date = resolvedExpireDate();
    return date == null ? "" : date.toString();
  }

  public String expireStampLabel() {
    String label = expireDateLabel();
    return label.isBlank() ? "" : "Expires " + label;
  }

  public String validityCopy() {
    return BookingPresentation.quoteValidityCopy(resolvedExpireDate());
  }

  public boolean hasBooking() {
    return bookingRefNum != null && !bookingRefNum.isBlank();
  }

  public String editUrl() {
    String path = directCustomer ? "/parks/direct" : "/parks";
    return path + "?quote=" + (quoteRefNum == null ? "" : quoteRefNum);
  }

  public String contentEditUrl() {
    if (directCustomer) {
      return editUrl();
    }
    return "/parks/quote/edit?quote=" + (quoteRefNum == null ? "" : quoteRefNum);
  }

  public QuoteMainView withDetails(List<QuoteDetailView> nextDetails) {
    return new QuoteMainView(
        quoteRefNum,
        userName,
        userId,
        companyName,
        companyId,
        value,
        currency,
        createdDate,
        updatedDate,
        status,
        customerName,
        customerEmail,
        customerPhone,
        followUp,
        sentTo,
        sentAt,
        markupPercent,
        subtotal,
        markupAmount,
        roeRate,
        convertedValue,
        bookingRefNum,
        canEditItems,
        nextDetails,
        packageInclusions,
        packageExclusions,
        guestInformation,
        dayPlans,
        directCustomer,
        expireDate);
  }

  public String bookingRefLabel() {
    return hasBooking() ? bookingRefNum : "";
  }

  public String searchText() {
    StringJoiner joiner = new StringJoiner(" ");
    if (quoteRefNum != null) {
      joiner.add(quoteRefNum);
    }
    if (bookingRefNum != null) {
      joiner.add(bookingRefNum);
    }
    if (userName != null) {
      joiner.add(userName);
    }
    if (customerName != null) {
      joiner.add(customerName);
    }
    if (customerEmail != null) {
      joiner.add(customerEmail);
    }
    if (sentTo != null) {
      joiner.add(sentTo);
    }
    if (companyName != null) {
      joiner.add(companyName);
    }
    if (details != null) {
      for (QuoteDetailView detail : details) {
        if (detail.itemName() != null) {
          joiner.add(detail.itemName());
        }
      }
    }
    return joiner.toString().toLowerCase(Locale.ROOT);
  }
}

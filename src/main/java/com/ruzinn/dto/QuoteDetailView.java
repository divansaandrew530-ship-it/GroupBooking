package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record QuoteDetailView(
    Long id,
    String quoteRefNum,
    String itemId,
    String itemName,
    String kind,
    String category,
    LocalDate visitDate,
    LocalDate checkoutDate,
    int adultCount,
    int childCount,
    int rooms,
    String roomType,
    String timeSlot,
    BigDecimal lineTotal,
    BigDecimal adultRate,
    BigDecimal childRate,
    BigDecimal actualLineTotal,
    BigDecimal actualAdultRate,
    BigDecimal actualChildRate,
    String description,
    String termsAndCondition,
    String included,
    String excluded,
    BigDecimal roeRate,
    BigDecimal convertedRate,
    BigDecimal actualConvertedRate,
    String currency) {

  public String kindLabel() {
    if ("stay".equalsIgnoreCase(kind) || "hotels".equalsIgnoreCase(category) || "hotel".equalsIgnoreCase(category)) {
      return "Hotel";
    }
    if ("experience".equalsIgnoreCase(kind)) {
      return "Park";
    }
    if ("visa".equalsIgnoreCase(kind) || "visa".equalsIgnoreCase(category)) {
      return "Visa";
    }
    if ("transfer".equalsIgnoreCase(kind) || "transfer".equalsIgnoreCase(category)) {
      return "Transfer";
    }
    return kind == null || kind.isBlank() ? "Item" : kind;
  }

  public String travelDateLabel() {
    if (visitDate == null) {
      return "";
    }
    String label = BookingPresentation.formatTravel(visitDate);
    if (checkoutDate != null) {
      label += " → " + BookingPresentation.formatTravel(checkoutDate);
    }
    return label;
  }

  public String guestsLabel() {
    return adultCount + " adult(s) · " + childCount + " child(ren)";
  }

  public String amountLabel() {
    return BookingPresentation.formatAmount(
        convertedRate != null ? convertedRate : lineTotal, currency);
  }

  public String roeRateLabel() {
    return BookingPresentation.formatRoe(currency, roeRate);
  }

  public String convertedAmountLabel() {
    return BookingPresentation.formatAmount(
        convertedRate != null ? convertedRate : lineTotal, currency);
  }

  public String detailsLabel() {
    StringBuilder details = new StringBuilder();
    String travel = travelDateLabel();
    if (!travel.isBlank()) {
      details.append(travel);
    }
    if (roomType != null && !roomType.isBlank()) {
      if (details.length() > 0) {
        details.append(" · ");
      }
      details.append(roomType.trim());
      if (rooms > 0) {
        details.append(" · ").append(rooms).append(rooms == 1 ? " room" : " rooms");
      }
    }
    if (timeSlot != null && !timeSlot.isBlank()) {
      if (details.length() > 0) {
        details.append(" · ");
      }
      details.append("Timeslot ").append(timeSlot.trim());
    }
    return details.toString();
  }

  public String itemSummaryLabel() {
    return kindLabel() + " · " + guestsLabel();
  }

  public String displayVisitDate() {
    return ParkQuoteLine.formatDate(visitDate == null ? null : visitDate.toString());
  }

  public String descriptionLabel() {
    return description == null ? "" : description;
  }

  public String termsLabel() {
    return termsAndCondition == null ? "" : termsAndCondition;
  }

  public String includedLabel() {
    return included == null ? "" : included;
  }

  public String excludedLabel() {
    return excluded == null ? "" : excluded;
  }

  public String adultRateLabel() {
    return adultRate == null ? "" : adultRate.stripTrailingZeros().toPlainString();
  }

  public String childRateLabel() {
    return childRate == null ? "" : childRate.stripTrailingZeros().toPlainString();
  }

  public String actualAdultRateLabel() {
    return actualAdultRate == null ? "" : actualAdultRate.stripTrailingZeros().toPlainString();
  }

  public String actualChildRateLabel() {
    return actualChildRate == null ? "" : actualChildRate.stripTrailingZeros().toPlainString();
  }

  public String actualAmountLabel() {
    return BookingPresentation.formatAmount(
        actualConvertedRate != null ? actualConvertedRate : actualLineTotal, currency);
  }

  public boolean hasActualAmount() {
    return actualConvertedRate != null || actualLineTotal != null || actualAdultRate != null;
  }

  public String actualRateSummaryLabel() {
    String adult = actualAdultRateLabel();
    if (stay() || actualChildRate == null) {
      return adult;
    }
    String child = actualChildRateLabel();
    if (adult.isBlank()) {
      return child;
    }
    if (child.isBlank()) {
      return adult;
    }
    return adult + " / " + child;
  }

  public String quotedAmountLabel() {
    return amountLabel();
  }

  public String rateInputValue() {
    if (adultRate != null) {
      return adultRate.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
    return lineTotal == null ? "0.00" : lineTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  public boolean stay() {
    return "stay".equalsIgnoreCase(kind)
        || "hotels".equalsIgnoreCase(category)
        || "hotel".equalsIgnoreCase(category);
  }

  public String visitDateIso() {
    return visitDate == null ? "" : visitDate.toString();
  }

  public String checkoutDateIso() {
    return checkoutDate == null ? "" : checkoutDate.toString();
  }

  public QuoteDetailView withContentDefaults(
      String nextDescription,
      String nextTerms,
      String nextIncluded,
      String nextExcluded,
      BigDecimal nextAdultRate,
      BigDecimal nextChildRate) {
    return new QuoteDetailView(
        id,
        quoteRefNum,
        itemId,
        itemName,
        kind,
        category,
        visitDate,
        checkoutDate,
        adultCount,
        childCount,
        rooms,
        roomType,
        timeSlot,
        lineTotal,
        adultRate != null ? adultRate : nextAdultRate,
        childRate != null ? childRate : nextChildRate,
        actualLineTotal,
        actualAdultRate != null ? actualAdultRate : nextAdultRate,
        actualChildRate != null ? actualChildRate : nextChildRate,
        firstNonBlank(description, nextDescription),
        firstNonBlank(termsAndCondition, nextTerms),
        firstNonBlank(included, nextIncluded),
        firstNonBlank(excluded, nextExcluded),
        roeRate,
        convertedRate,
        actualConvertedRate,
        currency);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }
}

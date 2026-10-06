package com.ruzinn.dto;

import com.ruzinn.entity.RoeMaster;
import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.Instant;

public record RoeRateView(String currency, BigDecimal rate, String label, Instant updatedDate, String updatedBy) {

  public static RoeRateView aed() {
    return new RoeRateView(
        "AED",
        BigDecimal.ONE.setScale(6),
        BookingPresentation.formatRoe("AED", BigDecimal.ONE),
        null,
        null);
  }

  public static RoeRateView from(RoeMaster row) {
    if (row == null) {
      return aed();
    }
    return new RoeRateView(
        row.getCurrency(),
        row.getRate(),
        BookingPresentation.formatRoe(row.getCurrency(), row.getRate()),
        row.getUpdatedDate(),
        row.getUpdatedBy());
  }

  public String updatedDateLabel() {
    return BookingPresentation.formatCreated(updatedDate);
  }

  public String rateInputValue() {
    if (rate == null) {
      return "";
    }
    return rate.stripTrailingZeros().toPlainString();
  }
}

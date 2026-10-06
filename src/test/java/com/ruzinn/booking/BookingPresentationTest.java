package com.ruzinn.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruzinn.entity.BookingDetail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingPresentationTest {

  @Test
  void mapsKnownStatusStringsAndFormatsAmounts() {
    assertThat(BookingPresentation.label("confirmed")).isEqualTo("Confirmed");
    assertThat(BookingPresentation.tone("PENDING")).isEqualTo("pending");
    assertThat(BookingPresentation.label("canceled")).isEqualTo("Cancelled");
    assertThat(BookingPresentation.formatAmount(new BigDecimal("1935.00"))).isEqualTo("AED 1,935");
    assertThat(BookingPresentation.formatAmount(new BigDecimal("12.25"), "USD")).isEqualTo("USD 12.25");
    assertThat(BookingPresentation.formatRoe("INR", new BigDecimal("22.7"))).isEqualTo("1 AED = 22.7 INR");
  }

  @Test
  void pastVisitDatesAreCompletedAndFutureDatesStayConfirmed() {
    BookingMain past = bookingWithShowDate(LocalDate.of(2020, 1, 10));
    BookingMain future = bookingWithShowDate(LocalDate.of(2099, 9, 21));
    assertThat(BookingPresentation.deriveStatus(past)).isEqualTo(BookingPresentation.COMPLETED);
    assertThat(BookingPresentation.deriveStatus(future)).isEqualTo(BookingPresentation.CONFIRMED);
    assertThat(BookingPresentation.formatCreated(Instant.parse("2026-09-15T05:42:00Z")))
        .isEqualTo("15-Sep-2026, 09:42");
    assertThat(BookingPresentation.formatTravel(LocalDate.of(2026, 10, 1))).isEqualTo("01-Oct-2026");
  }

  @Test
  void rollupPrefersAttentionThenCompletedOrConfirmed() {
    assertThat(BookingPresentation.rollupStatus(java.util.List.of("PENDING", "CONFIRMED")))
        .isEqualTo(BookingPresentation.PENDING);
    assertThat(BookingPresentation.rollupStatus(java.util.List.of("REQUESTED", "COMPLETED")))
        .isEqualTo(BookingPresentation.REQUESTED);
    assertThat(BookingPresentation.rollupStatus(java.util.List.of("COMPLETED", "COMPLETED")))
        .isEqualTo(BookingPresentation.COMPLETED);
    assertThat(BookingPresentation.rollupStatus(java.util.List.of("CANCELLED", "CANCELLED")))
        .isEqualTo(BookingPresentation.CANCELLED);
    assertThat(BookingPresentation.documentKind("HOTEL")).isEqualTo("voucher");
    assertThat(BookingPresentation.documentKind("ATTRACTION")).isEqualTo("ticket");
    assertThat(BookingPresentation.documentKind("VISA")).isEqualTo("visa");
    assertThat(BookingPresentation.documentLabel("voucher")).isEqualTo("Voucher");
    assertThat(BookingPresentation.documentLabel("ticket")).isEqualTo("Ticket");
    assertThat(BookingPresentation.documentLabel("visa copy")).isEqualTo("Visa copy");
  }

  private static BookingMain bookingWithShowDate(LocalDate showDate) {
    BookingMain main = new BookingMain();
    BookingDetail detail = new BookingDetail();
    detail.setShowDate(showDate);
    main.addDetail(detail);
    return main;
  }
}

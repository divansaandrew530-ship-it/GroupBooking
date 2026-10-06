package com.ruzinn.service;

import com.ruzinn.config.MailProperties;
import com.ruzinn.dto.BookingMailModel;
import com.ruzinn.dto.MailLineView;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteMailModel;
import com.ruzinn.entity.BookingDetail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.QuoteDetail;
import com.ruzinn.entity.QuoteMain;
import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class MailTemplateRenderer {

  private final TemplateEngine templateEngine;
  private final MailProperties mailProperties;

  public MailTemplateRenderer(TemplateEngine templateEngine, MailProperties mailProperties) {
    this.templateEngine = templateEngine;
    this.mailProperties = mailProperties;
  }

  public String renderQuote(ParkQuote quote, ParkQuoteRequest request) {
    return templateEngine.process("mail/quote", quoteContext(model(quote, request)));
  }

  public String renderBooking(ParkBooking booking, ParkQuoteRequest request, String paymentNote) {
    return templateEngine.process("mail/booking-confirmation", bookingContext(model(booking, request, paymentNote)));
  }

  public String renderBooking(BookingMain booking, QuoteMain quote) {
    return templateEngine.process("mail/booking-confirmation", bookingContext(model(booking, quote)));
  }

  public QuoteMailModel model(ParkQuote quote, ParkQuoteRequest request) {
    List<MailLineView> lines = new ArrayList<>();
    if (quote != null && quote.getItems() != null) {
      for (ParkQuoteLine line : quote.getItems()) {
        lines.add(
            new MailLineView(
                nullToEmpty(line.getName()),
                nullToEmpty(line.getKindLabel()),
                details(line),
                nullToEmpty(line.getGuestSummary()),
                money(line.getLineTotal())));
      }
    }
    String guestName = request == null ? "" : nullToEmpty(request.getLeadName());
    String guestEmail = request == null ? "" : nullToEmpty(request.getEmail());
    return new QuoteMailModel(
        mailProperties.logoUrl(),
        quote == null ? "" : nullToEmpty(quote.getQuoteNumber()),
        quote == null ? "" : nullToEmpty(quote.getIssuedOn()),
        quote == null ? BookingPresentation.formatTravel(BookingPresentation.defaultQuoteExpireDate()) : quote.getExpireDateLabel(),
        quote == null ? "Rathin" : firstNonBlank(quote.getPreparedBy(), "Rathin"),
        quote == null ? "" : nullToEmpty(quote.getCompanyName()),
        guestName,
        guestEmail,
        quote == null || quote.getCurrency() == null || quote.getCurrency().isBlank()
            ? "AED"
            : quote.getCurrency(),
        lines,
        money(quote == null ? 0 : quote.getSubtotal()),
        money(quote == null ? 0 : quote.getMarkupAmount()),
        money(quote == null ? 0 : quote.getTotal()));
  }

  public BookingMailModel model(ParkBooking booking, ParkQuoteRequest request, String paymentNote) {
    ParkQuote quote = booking == null ? null : booking.getQuote();
    List<MailLineView> lines = new ArrayList<>();
    if (quote != null && quote.getItems() != null) {
      for (ParkQuoteLine line : quote.getItems()) {
        lines.add(
            new MailLineView(
                nullToEmpty(line.getName()),
                nullToEmpty(line.getKindLabel()),
                details(line),
                nullToEmpty(line.getGuestSummary()),
                money(line.getLineTotal())));
      }
    }
    String guestName =
        firstNonBlank(
            booking == null ? null : booking.getLeadName(),
            request == null ? null : request.getLeadName());
    String guestEmail =
        firstNonBlank(
            booking == null ? null : booking.getEmail(),
            request == null ? null : request.getEmail());
    String guestPhone =
        firstNonBlank(
            booking == null ? null : booking.getPhone(),
            request == null ? null : request.getPhone());
    return new BookingMailModel(
        mailProperties.logoUrl(),
        booking == null ? "" : nullToEmpty(booking.getBookingRef()),
        booking == null ? "" : nullToEmpty(booking.getConfirmationId()),
        BookingPresentation.formatCreated(Instant.now()),
        guestName,
        guestEmail,
        guestPhone,
        quote == null ? "" : nullToEmpty(quote.getCompanyName()),
        paymentNote == null || paymentNote.isBlank() ? "Paid" : paymentNote.trim(),
        quote == null || quote.getCurrency() == null || quote.getCurrency().isBlank()
            ? "AED"
            : quote.getCurrency(),
        lines,
        money(quote == null ? 0 : quote.getTotal()),
        // A checkout booking starts PENDING: this mail says it was received and awaits approval.
        false);
  }

  public BookingMailModel model(BookingMain booking, QuoteMain quote) {
    List<MailLineView> lines = new ArrayList<>();
    if (booking != null && booking.getDetails() != null) {
      for (BookingDetail detail : booking.getDetails()) {
        lines.add(
            new MailLineView(
                nullToEmpty(detail.getItemName()),
                kindLabel(detail.getInventoryType(), detail.getServiceName()),
                details(detail),
                guests(detail.getAdultCount(), detail.getChildCount()),
                money(
                    detail.getConvertedRate() != null
                        ? detail.getConvertedRate().doubleValue()
                        : detail.getTotalRate() == null ? 0 : detail.getTotalRate().doubleValue())));
      }
    } else if (quote != null && quote.getDetails() != null) {
      for (QuoteDetail detail : quote.getDetails()) {
        lines.add(
            new MailLineView(
                nullToEmpty(detail.getItemName()),
                kindLabel(detail.getKind(), null),
                details(detail),
                guests(detail.getAdultCount(), detail.getChildCount()),
                money(
                    detail.getConvertedRate() != null
                        ? detail.getConvertedRate().doubleValue()
                        : detail.getLineTotal() == null ? 0 : detail.getLineTotal().doubleValue())));
      }
    }
    String currency =
        firstNonBlank(
            booking == null ? null : booking.getCurrency(),
            quote == null ? null : quote.getCurrency(),
            "AED");
    double total = 0;
    if (booking != null && booking.getConvertedValue() != null) {
      total = booking.getConvertedValue().doubleValue();
    } else if (quote != null && quote.getConvertedValue() != null) {
      total = quote.getConvertedValue().doubleValue();
    } else if (booking != null && booking.getValue() != null) {
      total = booking.getValue().doubleValue();
    } else if (quote != null && quote.getValue() != null) {
      total = quote.getValue().doubleValue();
    }
    return new BookingMailModel(
        mailProperties.logoUrl(),
        booking == null ? "" : nullToEmpty(booking.getBookingRefNum()),
        booking == null ? "" : nullToEmpty(booking.getBookingRefNum()),
        BookingPresentation.formatCreated(booking == null ? Instant.now() : booking.getCreatedDate()),
        firstNonBlank(booking == null ? null : booking.getUserName(), quote == null ? null : quote.getCustomerName()),
        firstNonBlank(
            booking == null ? null : booking.getLeadEmail(),
            quote == null ? null : quote.getCustomerEmail(),
            quote == null ? null : quote.getSentTo()),
        firstNonBlank(booking == null ? null : booking.getLeadPhone(), quote == null ? null : quote.getCustomerPhone()),
        firstNonBlank(booking == null ? null : booking.getCompanyName(), quote == null ? null : quote.getCompanyName()),
        "Paid",
        currency,
        lines,
        money(total),
        booking != null
            && BookingPresentation.CONFIRMED.equals(BookingPresentation.normalize(booking.getStatus())));
  }

  private Context quoteContext(QuoteMailModel model) {
    Context context = new Context(Locale.ENGLISH);
    context.setVariable("mail", model);
    return context;
  }

  private Context bookingContext(BookingMailModel model) {
    Context context = new Context(Locale.ENGLISH);
    context.setVariable("mail", model);
    return context;
  }

  static String details(ParkQuoteLine line) {
    if (line.isStay()) {
      StringBuilder details = new StringBuilder(line.getDisplayVisitDate());
      if (line.getCheckOut() != null) {
        details.append(" → ").append(line.getDisplayCheckOut());
      }
      if (line.getNights() != null) {
        details.append("\n").append(line.getNights()).append(line.getNights() == 1 ? " night" : " nights");
      }
      if (line.getRoomType() != null) {
        details.append("\n").append(line.getRoomType());
        if (line.getRooms() > 0) {
          details.append(" · ").append(line.getRooms()).append(line.getRooms() == 1 ? " room" : " rooms");
        }
      }
      return details.toString();
    }
    StringBuilder details = new StringBuilder(line.getDisplayVisitDate());
    if (line.getTimeSlot() != null && !line.getTimeSlot().isBlank()) {
      details.append("\nTimeslot ").append(line.getTimeSlot());
    }
    details.append("\n").append(line.getTicketSummary());
    return details.toString();
  }

  private static String details(BookingDetail detail) {
    StringBuilder details = new StringBuilder();
    LocalDate start = detail.getShowDate() != null ? detail.getShowDate() : detail.getCheckinDate();
    if (start != null) {
      details.append(BookingPresentation.formatTravel(start));
    }
    if (detail.getCheckoutDate() != null) {
      if (!details.isEmpty()) {
        details.append(" → ");
      }
      details.append(BookingPresentation.formatTravel(detail.getCheckoutDate()));
    }
    return details.toString();
  }

  private static String details(QuoteDetail detail) {
    StringBuilder details = new StringBuilder();
    if (detail.getVisitDate() != null) {
      details.append(BookingPresentation.formatTravel(detail.getVisitDate()));
    }
    if (detail.getCheckoutDate() != null) {
      if (!details.isEmpty()) {
        details.append(" → ");
      }
      details.append(BookingPresentation.formatTravel(detail.getCheckoutDate()));
    }
    if (detail.getTimeSlot() != null && !detail.getTimeSlot().isBlank()) {
      if (!details.isEmpty()) {
        details.append("\n");
      }
      details.append("Timeslot ").append(detail.getTimeSlot());
    }
    if (detail.getRoomType() != null && !detail.getRoomType().isBlank()) {
      if (!details.isEmpty()) {
        details.append("\n");
      }
      details.append(detail.getRoomType());
    }
    return details.toString();
  }

  private static String kindLabel(String kind, String fallback) {
    String value = kind == null ? "" : kind.trim();
    if (value.equalsIgnoreCase("stay") || value.equalsIgnoreCase("HOTEL")) {
      return "Hotel";
    }
    if (value.equalsIgnoreCase("experience") || value.equalsIgnoreCase("PARK") || value.equalsIgnoreCase("TICKET")) {
      return "Park";
    }
    if (value.equalsIgnoreCase("visa")) {
      return "Visa";
    }
    if (value.equalsIgnoreCase("transfer")) {
      return "Transfer";
    }
    if (!value.isBlank()) {
      return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1).toLowerCase(Locale.ENGLISH);
    }
    return fallback == null || fallback.isBlank() ? "Item" : fallback;
  }

  private static String guests(int adults, int children) {
    return adults
        + (adults == 1 ? " Adult" : " Adults")
        + ", "
        + children
        + (children == 1 ? " Child" : " Children");
  }

  static String money(double amount) {
    return String.format(Locale.US, "%,.2f", amount);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}

package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.BookingDetailView;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.DashboardKpis;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.DashboardService;
import com.ruzinn.service.ParkBookingService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Same rules as the Angular dashboard's client-side fallback ({@code dashboard-stats.ts}). */
@Service
public class DashboardServiceImpl implements DashboardService {

  private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

  private static final Set<String> NEEDS_ACTION = Set.of("PENDING", "REQUESTED");
  private static final Set<String> CLOSED = Set.of("CANCELLED", "CANCELED", "REJECTED");
  private static final Set<String> NOT_TICKETS = Set.of("HOTEL", "VISA");

  private final AccountService accountService;
  private final ParkBookingService bookingService;

  public DashboardServiceImpl(AccountService accountService, ParkBookingService bookingService) {
    this.accountService = accountService;
    this.bookingService = bookingService;
  }

  @Override
  @Transactional(readOnly = true)
  public DashboardKpis kpis(AuthSession actor) {
    AuthSession user = accountService.requireSignedIn(actor);
    List<BookingMainView> bookings =
        user.parentCompany() ? bookingService.listAllBookings() : bookingService.listCompanyBookings(user.companyId());
    Instant cutoff = LocalDate.now().minusDays(29).atStartOfDay(ZoneId.systemDefault()).toInstant();

    int last30Days = 0;
    int pendingParks = 0;
    int ticketsIssued = 0;
    int pendingTickets = 0;
    for (BookingMainView booking : bookings) {
      if (booking.createdDate() != null && !booking.createdDate().isBefore(cutoff)) {
        last30Days++;
      }
      boolean bookingHasTicket = hasText(booking.ticketFileName());
      for (BookingDetailView item : booking.details() == null ? List.<BookingDetailView>of() : booking.details()) {
        String status = normalize(item.status());
        if (CLOSED.contains(status)) {
          continue;
        }
        String inventory = normalize(item.inventoryType());
        boolean park = inventory.isEmpty() || inventory.equals("ATTRACTION");
        if (park && NEEDS_ACTION.contains(status)) {
          pendingParks++;
        }
        if (!NOT_TICKETS.contains(inventory)) {
          if (hasText(item.ticketFileName()) || bookingHasTicket) {
            ticketsIssued++;
          } else {
            pendingTickets++;
          }
        }
      }
    }
    DashboardKpis kpis = new DashboardKpis(last30Days, pendingParks, ticketsIssued, pendingTickets);
    log.debug("Dashboard for {} ({} booking(s)): {}", user.email(), bookings.size(), kpis);
    return kpis;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}

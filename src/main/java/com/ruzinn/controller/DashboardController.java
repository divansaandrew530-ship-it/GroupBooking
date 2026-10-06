package com.ruzinn.controller;

import com.ruzinn.service.DashboardService;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Home dashboard of the signed-in company. */
@RestController
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /** {@code {"dashboard": {"bookingsLast30Days": 3, "pendingParks": 1, "ticketsIssued": 2, "pendingTickets": 4}}}. */
  @GetMapping("/api/company/dashboard")
  public Map<String, Object> dashboard(HttpSession session) {
    return Map.of("dashboard", dashboardService.kpis(SessionUser.current(session)));
  }
}

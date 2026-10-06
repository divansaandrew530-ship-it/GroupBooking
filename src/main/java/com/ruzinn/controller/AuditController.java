package com.ruzinn.controller;

import com.ruzinn.dto.AuditReport;
import com.ruzinn.service.AuditReportService;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

  private final AuditReportService auditReportService;

  public AuditController(AuditReportService auditReportService) {
    this.auditReportService = auditReportService;
  }

  @GetMapping("/api/company/audit/bookings/{bookingRef}")
  public AuditReport booking(@PathVariable String bookingRef, HttpSession session) {
    return auditReportService.booking(SessionUser.current(session), bookingRef);
  }

  @GetMapping("/api/company/audit/bookings/{bookingRef}/items/{itemId}")
  public AuditReport bookingItem(
      @PathVariable String bookingRef, @PathVariable Long itemId, HttpSession session) {
    return auditReportService.bookingItem(SessionUser.current(session), bookingRef, itemId);
  }

  @GetMapping("/api/company/audit/quotes/{quoteRef}")
  public AuditReport quote(@PathVariable String quoteRef, HttpSession session) {
    return auditReportService.quote(SessionUser.current(session), quoteRef);
  }

  @GetMapping("/api/company/audit/users/{userId}")
  public AuditReport companyUser(@PathVariable String userId, HttpSession session) {
    return auditReportService.user(SessionUser.current(session), userId);
  }

  @GetMapping("/api/company/audit/companies/{companyId}")
  public AuditReport company(@PathVariable String companyId, HttpSession session) {
    return auditReportService.company(SessionUser.current(session), companyId);
  }

  @GetMapping("/api/admin/audit/companies/{companyId}")
  public AuditReport adminCompany(@PathVariable String companyId, HttpSession session) {
    return auditReportService.company(SessionUser.current(session), companyId);
  }

  @GetMapping("/api/admin/audit/users/{userId}")
  public AuditReport adminUser(@PathVariable String userId, HttpSession session) {
    return auditReportService.user(SessionUser.current(session), userId);
  }

  @GetMapping("/api/admin/audit/login-home")
  public AuditReport loginHome(HttpSession session) {
    return auditReportService.loginHome(SessionUser.current(session));
  }

  @GetMapping("/api/admin/audit/items/{itemId}")
  public AuditReport adminItem(@PathVariable String itemId, HttpSession session) {
    return auditReportService.item(SessionUser.current(session), itemId);
  }
}

package com.ruzinn.service;

import com.ruzinn.dto.AuditReport;
import com.ruzinn.dto.AuthSession;

public interface AuditReportService {

  AuditReport booking(AuthSession actor, String bookingRefNum);

  AuditReport bookingItem(AuthSession actor, String bookingRefNum, Long itemId);

  AuditReport company(AuthSession actor, String companyId);

  AuditReport user(AuthSession actor, String userId);

  AuditReport loginHome(AuthSession actor);

  AuditReport item(AuthSession actor, String itemId);

  AuditReport quote(AuthSession actor, String quoteRefNum);
}

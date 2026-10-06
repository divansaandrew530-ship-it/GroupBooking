package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.DashboardKpis;

public interface DashboardService {

  /** KPIs over the bookings the user may see (own company, or all for parent-company users). */
  DashboardKpis kpis(AuthSession actor);
}

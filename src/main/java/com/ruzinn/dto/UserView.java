package com.ruzinn.dto;

import java.time.Instant;

public record UserView(
    String userId,
    String userName,
    String email,
    String role,
    Long companyId,
    String companyName,
    String companyStatus,
    String status,
    boolean emailVerified,
    Instant createdDate) {

  public boolean blocked() {
    return "DISABLED".equals(status);
  }

  public boolean superAdmin() {
    return "SUPER_ADMIN".equals(role);
  }
}

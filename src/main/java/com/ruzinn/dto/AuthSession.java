package com.ruzinn.dto;

public record AuthSession(
    String userId,
    String userName,
    String email,
    String role,
    String companyId,
    String companyName,
    String companyStatus,
    String companyType,
    boolean emailVerified) {

  public boolean superAdmin() {
    return "SUPER_ADMIN".equals(role);
  }

  public boolean companyAdmin() {
    return superAdmin() || "COMPANY_ADMIN".equals(role);
  }

  public boolean companyActive() {
    return superAdmin() || "ACTIVE".equals(companyStatus);
  }

  public boolean parentCompany() {
    return superAdmin() || "PARENT".equalsIgnoreCase(companyType == null ? "" : companyType.trim());
  }

  public boolean parentCompanyAdmin() {
    return superAdmin() || (parentCompany() && "COMPANY_ADMIN".equals(role));
  }

  public boolean canManageBookings() {
    return parentCompany();
  }

  public boolean canManageQuotes() {
    return companyActive();
  }
}

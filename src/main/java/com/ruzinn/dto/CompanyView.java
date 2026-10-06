package com.ruzinn.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CompanyView(
    Long companyId,
    String companyName,
    String email,
    String phone,
    String status,
    String companyType,
    boolean emailVerified,
    Instant createdDate,
    Instant reviewedDate,
    String reviewedBy,
    String reviewNote,
    BigDecimal creditLimit,
    BigDecimal creditUsed,
    String registrationNo,
    String address,
    String website,
    String country,
    String city,
    String zipCode,
    String ownerName,
    String ownerTelephone,
    String operationsStaffName,
    String operationsStaffNumber,
    String financeName,
    String financeMobile,
    String financeTelephone,
    String financeEmail,
    boolean hasVatCertificate,
    String vatFileName,
    boolean hasLicence,
    String licenceFileName,
    boolean hasLogo,
    String logoFileName,
    String logoUrl) {

  public boolean parentCompany() {
    return "PARENT".equalsIgnoreCase(companyType == null ? "" : companyType.trim());
  }

  public boolean hasProfile() {
    return notBlank(registrationNo)
        || notBlank(address)
        || notBlank(website)
        || notBlank(country)
        || notBlank(city)
        || notBlank(zipCode)
        || notBlank(ownerName)
        || notBlank(ownerTelephone)
        || notBlank(operationsStaffName)
        || notBlank(operationsStaffNumber)
        || notBlank(financeName)
        || notBlank(financeMobile)
        || notBlank(financeTelephone)
        || notBlank(financeEmail)
        || hasVatCertificate
        || hasLicence
        || hasLogo;
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}

package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "company",
    indexes = {
      @Index(name = "idx_company_email", columnList = "email", unique = true),
      @Index(name = "idx_company_status", columnList = "status")
    })
public class Company {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_REJECTED = "REJECTED";
  public static final String TYPE_PARENT = "PARENT";
  public static final String TYPE_CHILD = "CHILD";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "company_id", nullable = false, columnDefinition = "bigint")
  private Long companyId;

  @Column(name = "company_name", nullable = false, length = 160)
  private String companyName;

  @Column(name = "email", nullable = false, length = 160)
  private String email;

  @Column(name = "phone", length = 40)
  private String phone;

  @Column(name = "status", nullable = false, length = 20)
  private String status = STATUS_PENDING;

  @org.hibernate.annotations.ColumnDefault("'CHILD'")
  @Column(name = "company_type", nullable = false, length = 20)
  private String companyType = TYPE_CHILD;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @Column(name = "reviewed_date")
  private Instant reviewedDate;

  @Column(name = "reviewed_by", length = 160)
  private String reviewedBy;

  @Column(name = "review_note", length = 400)
  private String reviewNote;

  @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
  private BigDecimal creditLimit = BigDecimal.ZERO;

  @Column(name = "credit_used", nullable = false, precision = 12, scale = 2)
  private BigDecimal creditUsed = BigDecimal.ZERO;

  @Column(name = "registration_no", length = 80)
  private String registrationNo = "";

  @Column(name = "address", length = 400)
  private String address = "";

  @Column(name = "website", length = 200)
  private String website = "";

  @Column(name = "country", length = 80)
  private String country = "";

  @Column(name = "city", length = 80)
  private String city = "";

  @Column(name = "zip_code", length = 20)
  private String zipCode = "";

  @Column(name = "owner_name", length = 160)
  private String ownerName = "";

  @Column(name = "owner_telephone", length = 40)
  private String ownerTelephone = "";

  @Column(name = "operations_staff_name", length = 160)
  private String operationsStaffName = "";

  @Column(name = "operations_staff_number", length = 40)
  private String operationsStaffNumber = "";

  @Column(name = "finance_name", length = 160)
  private String financeName = "";

  @Column(name = "finance_mobile", length = 40)
  private String financeMobile = "";

  @Column(name = "finance_telephone", length = 40)
  private String financeTelephone = "";

  @Column(name = "finance_email", length = 160)
  private String financeEmail = "";

  @Column(name = "vat_file_name", length = 255)
  private String vatFileName;

  @Column(name = "vat_content_type", length = 120)
  private String vatContentType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "vat_bytes", length = 16_777_216)
  private byte[] vatBytes;

  @Column(name = "licence_file_name", length = 255)
  private String licenceFileName;

  @Column(name = "licence_content_type", length = 120)
  private String licenceContentType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "licence_bytes", length = 16_777_216)
  private byte[] licenceBytes;

  @Column(name = "logo_file_name", length = 255)
  private String logoFileName;

  @Column(name = "logo_content_type", length = 120)
  private String logoContentType;

  @Column(name = "logo_path", length = 400)
  private String logoPath;

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCompanyType() {
    return normalizeType(companyType);
  }

  public void setCompanyType(String companyType) {
    this.companyType = normalizeType(companyType);
  }

  public boolean parentCompany() {
    return TYPE_PARENT.equals(getCompanyType());
  }

  public static String normalizeType(String companyType) {
    return TYPE_PARENT.equalsIgnoreCase(companyType == null ? "" : companyType.trim())
        ? TYPE_PARENT
        : TYPE_CHILD;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Instant getReviewedDate() {
    return reviewedDate;
  }

  public void setReviewedDate(Instant reviewedDate) {
    this.reviewedDate = reviewedDate;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String reviewNote) {
    this.reviewNote = reviewNote;
  }

  public BigDecimal getCreditLimit() {
    return creditLimit;
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit == null ? BigDecimal.ZERO : creditLimit;
  }

  public BigDecimal getCreditUsed() {
    return creditUsed;
  }

  public void setCreditUsed(BigDecimal creditUsed) {
    this.creditUsed = creditUsed == null ? BigDecimal.ZERO : creditUsed;
  }

  public String getRegistrationNo() {
    return registrationNo;
  }

  public void setRegistrationNo(String registrationNo) {
    this.registrationNo = registrationNo == null ? "" : registrationNo;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address == null ? "" : address;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website == null ? "" : website;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country == null ? "" : country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city == null ? "" : city;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode == null ? "" : zipCode;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName == null ? "" : ownerName;
  }

  public String getOwnerTelephone() {
    return ownerTelephone;
  }

  public void setOwnerTelephone(String ownerTelephone) {
    this.ownerTelephone = ownerTelephone == null ? "" : ownerTelephone;
  }

  public String getOperationsStaffName() {
    return operationsStaffName;
  }

  public void setOperationsStaffName(String operationsStaffName) {
    this.operationsStaffName = operationsStaffName == null ? "" : operationsStaffName;
  }

  public String getOperationsStaffNumber() {
    return operationsStaffNumber;
  }

  public void setOperationsStaffNumber(String operationsStaffNumber) {
    this.operationsStaffNumber = operationsStaffNumber == null ? "" : operationsStaffNumber;
  }

  public String getFinanceName() {
    return financeName;
  }

  public void setFinanceName(String financeName) {
    this.financeName = financeName == null ? "" : financeName;
  }

  public String getFinanceMobile() {
    return financeMobile;
  }

  public void setFinanceMobile(String financeMobile) {
    this.financeMobile = financeMobile == null ? "" : financeMobile;
  }

  public String getFinanceTelephone() {
    return financeTelephone;
  }

  public void setFinanceTelephone(String financeTelephone) {
    this.financeTelephone = financeTelephone == null ? "" : financeTelephone;
  }

  public String getFinanceEmail() {
    return financeEmail;
  }

  public void setFinanceEmail(String financeEmail) {
    this.financeEmail = financeEmail == null ? "" : financeEmail;
  }

  public String getVatFileName() {
    return vatFileName;
  }

  public void setVatFileName(String vatFileName) {
    this.vatFileName = vatFileName;
  }

  public String getVatContentType() {
    return vatContentType;
  }

  public void setVatContentType(String vatContentType) {
    this.vatContentType = vatContentType;
  }

  public byte[] getVatBytes() {
    return vatBytes;
  }

  public void setVatBytes(byte[] vatBytes) {
    this.vatBytes = vatBytes;
  }

  public String getLicenceFileName() {
    return licenceFileName;
  }

  public void setLicenceFileName(String licenceFileName) {
    this.licenceFileName = licenceFileName;
  }

  public String getLicenceContentType() {
    return licenceContentType;
  }

  public void setLicenceContentType(String licenceContentType) {
    this.licenceContentType = licenceContentType;
  }

  public byte[] getLicenceBytes() {
    return licenceBytes;
  }

  public void setLicenceBytes(byte[] licenceBytes) {
    this.licenceBytes = licenceBytes;
  }

  public boolean hasVatCertificate() {
    return vatBytes != null && vatBytes.length > 0;
  }

  public boolean hasLicence() {
    return licenceBytes != null && licenceBytes.length > 0;
  }

  public String getLogoFileName() {
    return logoFileName;
  }

  public void setLogoFileName(String logoFileName) {
    this.logoFileName = logoFileName;
  }

  public String getLogoContentType() {
    return logoContentType;
  }

  public void setLogoContentType(String logoContentType) {
    this.logoContentType = logoContentType;
  }

  public String getLogoPath() {
    return logoPath;
  }

  public void setLogoPath(String logoPath) {
    this.logoPath = logoPath;
  }

  public boolean hasLogo() {
    return logoPath != null && !logoPath.isBlank();
  }

  public BigDecimal remainingCredit() {
    return getCreditLimit().subtract(getCreditUsed());
  }
}

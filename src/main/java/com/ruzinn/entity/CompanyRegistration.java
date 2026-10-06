package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "company_registration",
    indexes = {
      @Index(name = "idx_company_registration_email", columnList = "owner_email", unique = true),
      @Index(name = "idx_company_registration_company", columnList = "company_id")
    })
public class CompanyRegistration {

  public static final String STATUS_DRAFT = "DRAFT";
  public static final String STATUS_SUBMITTED = "SUBMITTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, columnDefinition = "bigint")
  private Long companyId;

  @Column(name = "company_name", nullable = false, length = 160)
  private String companyName = "";

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

  @Column(name = "owner_mobile", length = 40)
  private String ownerMobile = "";

  @Column(name = "owner_telephone", length = 40)
  private String ownerTelephone = "";

  @Column(name = "owner_email", nullable = false, length = 160)
  private String ownerEmail = "";

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

  @Column(name = "otp_verified", nullable = false)
  private boolean otpVerified;

  @Column(name = "status", nullable = false, length = 20)
  private String status = STATUS_DRAFT;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @Column(name = "submitted_date")
  private Instant submittedDate;

  public Long getId() {
    return id;
  }

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
    this.companyName = companyName == null ? "" : companyName;
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

  public String getOwnerMobile() {
    return ownerMobile;
  }

  public void setOwnerMobile(String ownerMobile) {
    this.ownerMobile = ownerMobile == null ? "" : ownerMobile;
  }

  public String getOwnerTelephone() {
    return ownerTelephone;
  }

  public void setOwnerTelephone(String ownerTelephone) {
    this.ownerTelephone = ownerTelephone == null ? "" : ownerTelephone;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail == null ? "" : ownerEmail;
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

  public boolean isOtpVerified() {
    return otpVerified;
  }

  public void setOtpVerified(boolean otpVerified) {
    this.otpVerified = otpVerified;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status == null || status.isBlank() ? STATUS_DRAFT : status;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Instant getSubmittedDate() {
    return submittedDate;
  }

  public void setSubmittedDate(Instant submittedDate) {
    this.submittedDate = submittedDate;
  }
}

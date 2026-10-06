package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "app_user",
    indexes = {
      @Index(name = "idx_app_user_email", columnList = "email", unique = true),
      @Index(name = "idx_app_user_company", columnList = "company_id")
    })
public class AppUser {

  public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
  public static final String ROLE_COMPANY_ADMIN = "COMPANY_ADMIN";
  public static final String ROLE_COMPANY_USER = "COMPANY_USER";
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_DISABLED = "DISABLED";

  @Id
  @Column(name = "user_id", length = 32, nullable = false)
  private String userId;

  @Column(name = "user_name", nullable = false, length = 160)
  private String userName;

  @Column(name = "email", nullable = false, length = 160)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 80)
  private String passwordHash;

  @Column(name = "user_role", nullable = false, length = 32)
  private String role;

  @Column(name = "company_id", nullable = false, columnDefinition = "bigint")
  private Long companyId;

  @Column(name = "status", nullable = false, length = 20)
  private String status = STATUS_PENDING;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @Column(name = "created_by", length = 32)
  private String createdBy;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}

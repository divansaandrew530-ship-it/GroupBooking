package com.ruzinn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ruzinn.auth")
public class AuthProperties {

  private boolean otpEcho = true;
  private int otpTtlMinutes = 10;
  private String superAdminEmail = "Admin@rathin.com";
  private String superAdminPassword = "R@t@2023";
  private String superAdminName = "Rathin Super Admin";

  public boolean isOtpEcho() {
    return otpEcho;
  }

  public void setOtpEcho(boolean otpEcho) {
    this.otpEcho = otpEcho;
  }

  public int getOtpTtlMinutes() {
    return otpTtlMinutes;
  }

  public void setOtpTtlMinutes(int otpTtlMinutes) {
    this.otpTtlMinutes = otpTtlMinutes;
  }

  public String getSuperAdminEmail() {
    return superAdminEmail;
  }

  public void setSuperAdminEmail(String superAdminEmail) {
    this.superAdminEmail = superAdminEmail;
  }

  public String getSuperAdminPassword() {
    return superAdminPassword;
  }

  public void setSuperAdminPassword(String superAdminPassword) {
    this.superAdminPassword = superAdminPassword;
  }

  public String getSuperAdminName() {
    return superAdminName;
  }

  public void setSuperAdminName(String superAdminName) {
    this.superAdminName = superAdminName;
  }
}

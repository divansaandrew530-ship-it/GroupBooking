package com.ruzinn.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParkQuoteRequest {
  private int adults = 1;
  private int children = 0;
  private double markupPercent = 5;
  private List<ParkLineRequest> items = new ArrayList<>();
  private boolean directCustomer;
  private String packageInclusions = "";
  private String packageExclusions = "";
  private String guestInformation = "";
  private List<ParkDayPlan> dayPlans = new ArrayList<>();
  private String leadName;
  private String email;
  private String phone;
  private String userId;
  private String companyName;
  private String companyId;
  private String quoteNumber;
  private String country;
  private String contactUserId;
  private String ownerUserId;
  private String ownerUserName;
  private String currency;
  private LocalDate expireDate;

  public int getAdults() {
    return adults;
  }

  public void setAdults(int adults) {
    this.adults = adults;
  }

  public int getChildren() {
    return children;
  }

  public void setChildren(int children) {
    this.children = children;
  }

  public double getMarkupPercent() {
    return markupPercent;
  }

  public void setMarkupPercent(double markupPercent) {
    this.markupPercent = markupPercent;
  }

  public List<ParkLineRequest> getItems() {
    return items;
  }

  public void setItems(List<ParkLineRequest> items) {
    this.items = items;
  }

  public String getLeadName() {
    return leadName;
  }

  public void setLeadName(String leadName) {
    this.leadName = leadName;
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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getQuoteNumber() {
    return quoteNumber;
  }

  public void setQuoteNumber(String quoteNumber) {
    this.quoteNumber = quoteNumber;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getContactUserId() {
    return contactUserId;
  }

  public void setContactUserId(String contactUserId) {
    this.contactUserId = contactUserId;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getOwnerUserName() {
    return ownerUserName;
  }

  public void setOwnerUserName(String ownerUserName) {
    this.ownerUserName = ownerUserName;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public boolean isDirectCustomer() {
    return directCustomer;
  }

  public void setDirectCustomer(boolean directCustomer) {
    this.directCustomer = directCustomer;
  }

  public String getPackageInclusions() {
    return packageInclusions;
  }

  public void setPackageInclusions(String packageInclusions) {
    this.packageInclusions = packageInclusions;
  }

  public String getPackageExclusions() {
    return packageExclusions;
  }

  public void setPackageExclusions(String packageExclusions) {
    this.packageExclusions = packageExclusions;
  }

  public String getGuestInformation() {
    return guestInformation;
  }

  public void setGuestInformation(String guestInformation) {
    this.guestInformation = guestInformation;
  }

  public List<ParkDayPlan> getDayPlans() {
    return dayPlans;
  }

  public void setDayPlans(List<ParkDayPlan> dayPlans) {
    this.dayPlans = dayPlans == null ? new ArrayList<>() : dayPlans;
  }

  public LocalDate getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(LocalDate expireDate) {
    this.expireDate = expireDate;
  }
}

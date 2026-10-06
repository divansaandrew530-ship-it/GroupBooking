package com.ruzinn.entity;

import com.ruzinn.util.BookingPresentation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "quote_main",
    indexes = @Index(name = "idx_quote_main_company", columnList = "company_id"))
public class QuoteMain {

  @Id
  @Column(name = "quote_ref_num", length = 32, nullable = false)
  private String quoteRefNum;

  @Column(name = "user_name", length = 255)
  private String userName;

  @Column(name = "user_id", length = 128)
  private String userId;

  @Column(name = "company_name", length = 255)
  private String companyName;

  @Column(name = "company_id", length = 64)
  private String companyId;

  @Column(name = "quote_value", nullable = false, precision = 12, scale = 2)
  private BigDecimal value;

  @Column(name = "currency", length = 8)
  private String currency;

  @Column(name = "roe_rate", precision = 12, scale = 6)
  private BigDecimal roeRate;

  @Column(name = "converted_value", precision = 12, scale = 2)
  private BigDecimal convertedValue;

  @Column(name = "quote_status", length = 32)
  private String status;

  @Column(name = "customer_name", length = 255)
  private String customerName;

  @Column(name = "customer_email", length = 160)
  private String customerEmail;

  @Column(name = "customer_phone", length = 40)
  private String customerPhone;

  @Column(name = "follow_up", length = 500)
  private String followUp;

  @Column(name = "sent_to", length = 160)
  private String sentTo;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "markup_percent", precision = 6, scale = 2)
  private BigDecimal markupPercent;

  @Column(name = "subtotal", precision = 12, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "markup_amount", precision = 12, scale = 2)
  private BigDecimal markupAmount;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @Column(name = "expire_date")
  private LocalDate expireDate;

  @Column(name = "booking_ref_num", length = 32)
  private String bookingRefNum;

  @Column(name = "direct_customer")
  private Boolean directCustomer = Boolean.FALSE;

  @Column(name = "package_inclusions", columnDefinition = "text")
  private String packageInclusions = "";

  @Column(name = "package_exclusions", columnDefinition = "text")
  private String packageExclusions = "";

  @Column(name = "guest_information", columnDefinition = "text")
  private String guestInformation = "";

  @Column(name = "day_plans", columnDefinition = "text")
  private String dayPlansJson = "";

  @Column(name = "updated_date")
  private Instant updatedDate;

  @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private List<QuoteDetail> details = new ArrayList<>();

  public void addDetail(QuoteDetail detail) {
    details.add(detail);
    detail.setQuote(this);
  }

  public String getQuoteRefNum() {
    return quoteRefNum;
  }

  public void setQuoteRefNum(String quoteRefNum) {
    this.quoteRefNum = quoteRefNum;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
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

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public BigDecimal getRoeRate() {
    return roeRate;
  }

  public void setRoeRate(BigDecimal roeRate) {
    this.roeRate = roeRate;
  }

  public BigDecimal getConvertedValue() {
    return convertedValue;
  }

  public void setConvertedValue(BigDecimal convertedValue) {
    this.convertedValue = convertedValue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone;
  }

  public String getFollowUp() {
    return followUp;
  }

  public void setFollowUp(String followUp) {
    this.followUp = followUp;
  }

  public String getSentTo() {
    return sentTo;
  }

  public void setSentTo(String sentTo) {
    this.sentTo = sentTo;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public BigDecimal getMarkupPercent() {
    return markupPercent;
  }

  public void setMarkupPercent(BigDecimal markupPercent) {
    this.markupPercent = markupPercent;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public BigDecimal getMarkupAmount() {
    return markupAmount;
  }

  public void setMarkupAmount(BigDecimal markupAmount) {
    this.markupAmount = markupAmount;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public LocalDate getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(LocalDate expireDate) {
    this.expireDate = expireDate;
  }

  public LocalDate resolvedExpireDate() {
    return BookingPresentation.quoteExpireDate(expireDate, createdDate);
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getBookingRefNum() {
    return bookingRefNum;
  }

  public void setBookingRefNum(String bookingRefNum) {
    this.bookingRefNum = bookingRefNum;
  }

  public boolean isDirectCustomer() {
    return Boolean.TRUE.equals(directCustomer);
  }

  public void setDirectCustomer(boolean directCustomer) {
    this.directCustomer = directCustomer;
  }

  public String getPackageInclusions() {
    return packageInclusions;
  }

  public void setPackageInclusions(String packageInclusions) {
    this.packageInclusions = packageInclusions == null ? "" : packageInclusions;
  }

  public String getPackageExclusions() {
    return packageExclusions;
  }

  public void setPackageExclusions(String packageExclusions) {
    this.packageExclusions = packageExclusions == null ? "" : packageExclusions;
  }

  public String getGuestInformation() {
    return guestInformation;
  }

  public void setGuestInformation(String guestInformation) {
    this.guestInformation = guestInformation == null ? "" : guestInformation;
  }

  public String getDayPlansJson() {
    return dayPlansJson;
  }

  public void setDayPlansJson(String dayPlansJson) {
    this.dayPlansJson = dayPlansJson == null ? "" : dayPlansJson;
  }

  public List<QuoteDetail> getDetails() {
    return details;
  }

  public void setDetails(List<QuoteDetail> details) {
    this.details = details;
  }
}

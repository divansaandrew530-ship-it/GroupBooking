package com.ruzinn.entity;

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
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "booking_main",
    indexes = @Index(name = "idx_booking_main_company", columnList = "company_id"))
public class BookingMain {

  @Id
  @Column(name = "booking_ref_num", length = 32, nullable = false)
  private String bookingRefNum;

  @Column(name = "user_name", length = 255)
  private String userName;

  @Column(name = "user_id", length = 128)
  private String userId;

  @Column(name = "company_name", length = 255)
  private String companyName;

  @Column(name = "company_id", length = 64)
  private String companyId;

  @Column(name = "booking_value", nullable = false, precision = 12, scale = 2)
  private BigDecimal value;

  @Column(name = "currency", length = 8)
  private String currency;

  @Column(name = "roe_rate", precision = 12, scale = 6)
  private BigDecimal roeRate;

  @Column(name = "converted_value", precision = 12, scale = 2)
  private BigDecimal convertedValue;

  @Column(name = "booking_status", length = 32)
  private String status = "PENDING";

  @Column(name = "lead_email", length = 160)
  private String leadEmail;

  @Column(name = "lead_phone", length = 40)
  private String leadPhone;

  @Column(name = "voucher_file_name", length = 255)
  private String voucherFileName;

  @Column(name = "voucher_content_type", length = 120)
  private String voucherContentType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "voucher_bytes", length = 16_777_216)
  private byte[] voucherBytes;

  @Column(name = "ticket_file_name", length = 255)
  private String ticketFileName;

  @Column(name = "ticket_content_type", length = 120)
  private String ticketContentType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "ticket_bytes", length = 16_777_216)
  private byte[] ticketBytes;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private List<BookingDetail> details = new ArrayList<>();

  public void addDetail(BookingDetail detail) {
    details.add(detail);
    detail.setBooking(this);
  }

  public String getBookingRefNum() {
    return bookingRefNum;
  }

  public void setBookingRefNum(String bookingRefNum) {
    this.bookingRefNum = bookingRefNum;
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

  public String getLeadEmail() {
    return leadEmail;
  }

  public void setLeadEmail(String leadEmail) {
    this.leadEmail = leadEmail;
  }

  public String getLeadPhone() {
    return leadPhone;
  }

  public void setLeadPhone(String leadPhone) {
    this.leadPhone = leadPhone;
  }

  public String getVoucherFileName() {
    return voucherFileName;
  }

  public void setVoucherFileName(String voucherFileName) {
    this.voucherFileName = voucherFileName;
  }

  public String getVoucherContentType() {
    return voucherContentType;
  }

  public void setVoucherContentType(String voucherContentType) {
    this.voucherContentType = voucherContentType;
  }

  public byte[] getVoucherBytes() {
    return voucherBytes;
  }

  public void setVoucherBytes(byte[] voucherBytes) {
    this.voucherBytes = voucherBytes;
  }

  public String getTicketFileName() {
    return ticketFileName;
  }

  public void setTicketFileName(String ticketFileName) {
    this.ticketFileName = ticketFileName;
  }

  public String getTicketContentType() {
    return ticketContentType;
  }

  public void setTicketContentType(String ticketContentType) {
    this.ticketContentType = ticketContentType;
  }

  public byte[] getTicketBytes() {
    return ticketBytes;
  }

  public void setTicketBytes(byte[] ticketBytes) {
    this.ticketBytes = ticketBytes;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public List<BookingDetail> getDetails() {
    return details;
  }

  public void setDetails(List<BookingDetail> details) {
    this.details = details;
  }
}

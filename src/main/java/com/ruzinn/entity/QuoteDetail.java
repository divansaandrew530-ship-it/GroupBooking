package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "quote_details")
public class QuoteDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_ref_num", nullable = false)
  private QuoteMain quote;

  @Column(name = "item_id", length = 80)
  private String itemId;

  @Column(name = "item_name", nullable = false, length = 240)
  private String itemName;

  @Column(name = "kind", length = 40)
  private String kind;

  @Column(name = "category", length = 80)
  private String category;

  @Column(name = "visit_date")
  private LocalDate visitDate;

  @Column(name = "checkout_date")
  private LocalDate checkoutDate;

  @Column(name = "adult_count", nullable = false)
  private int adultCount;

  @Column(name = "child_count", nullable = false)
  private int childCount;

  @Column(name = "rooms")
  private int rooms;

  @Column(name = "room_type", length = 120)
  private String roomType;

  @Column(name = "time_slot", length = 80)
  private String timeSlot;

  @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineTotal;

  @Column(name = "roe_rate", precision = 12, scale = 6)
  private BigDecimal roeRate;

  @Column(name = "converted_rate", precision = 12, scale = 2)
  private BigDecimal convertedRate;

  @Column(name = "adult_rate", precision = 12, scale = 2)
  private BigDecimal adultRate;

  @Column(name = "child_rate", precision = 12, scale = 2)
  private BigDecimal childRate;

  @Column(name = "actual_adult_rate", precision = 12, scale = 2)
  private BigDecimal actualAdultRate;

  @Column(name = "actual_child_rate", precision = 12, scale = 2)
  private BigDecimal actualChildRate;

  @Column(name = "actual_line_total", precision = 12, scale = 2)
  private BigDecimal actualLineTotal;

  @Column(name = "actual_converted_rate", precision = 12, scale = 2)
  private BigDecimal actualConvertedRate;

  @Column(name = "description", columnDefinition = "text")
  private String description = "";

  @Column(name = "terms_and_condition", columnDefinition = "text")
  private String termsAndCondition = "";

  @Column(name = "included", columnDefinition = "text")
  private String included = "";

  @Column(name = "excluded", columnDefinition = "text")
  private String excluded = "";

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  public Long getId() {
    return id;
  }

  public QuoteMain getQuote() {
    return quote;
  }

  public void setQuote(QuoteMain quote) {
    this.quote = quote;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public LocalDate getVisitDate() {
    return visitDate;
  }

  public void setVisitDate(LocalDate visitDate) {
    this.visitDate = visitDate;
  }

  public LocalDate getCheckoutDate() {
    return checkoutDate;
  }

  public void setCheckoutDate(LocalDate checkoutDate) {
    this.checkoutDate = checkoutDate;
  }

  public int getAdultCount() {
    return adultCount;
  }

  public void setAdultCount(int adultCount) {
    this.adultCount = adultCount;
  }

  public int getChildCount() {
    return childCount;
  }

  public void setChildCount(int childCount) {
    this.childCount = childCount;
  }

  public int getRooms() {
    return rooms;
  }

  public void setRooms(int rooms) {
    this.rooms = rooms;
  }

  public String getRoomType() {
    return roomType;
  }

  public void setRoomType(String roomType) {
    this.roomType = roomType;
  }

  public String getTimeSlot() {
    return timeSlot;
  }

  public void setTimeSlot(String timeSlot) {
    this.timeSlot = timeSlot;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }

  public void setLineTotal(BigDecimal lineTotal) {
    this.lineTotal = lineTotal;
  }

  public BigDecimal getRoeRate() {
    return roeRate;
  }

  public void setRoeRate(BigDecimal roeRate) {
    this.roeRate = roeRate;
  }

  public BigDecimal getConvertedRate() {
    return convertedRate;
  }

  public void setConvertedRate(BigDecimal convertedRate) {
    this.convertedRate = convertedRate;
  }

  public BigDecimal getAdultRate() {
    return adultRate;
  }

  public void setAdultRate(BigDecimal adultRate) {
    this.adultRate = adultRate;
  }

  public BigDecimal getChildRate() {
    return childRate;
  }

  public void setChildRate(BigDecimal childRate) {
    this.childRate = childRate;
  }

  public BigDecimal getActualAdultRate() {
    return actualAdultRate;
  }

  public void setActualAdultRate(BigDecimal actualAdultRate) {
    this.actualAdultRate = actualAdultRate;
  }

  public BigDecimal getActualChildRate() {
    return actualChildRate;
  }

  public void setActualChildRate(BigDecimal actualChildRate) {
    this.actualChildRate = actualChildRate;
  }

  public BigDecimal getActualLineTotal() {
    return actualLineTotal;
  }

  public void setActualLineTotal(BigDecimal actualLineTotal) {
    this.actualLineTotal = actualLineTotal;
  }

  public BigDecimal getActualConvertedRate() {
    return actualConvertedRate;
  }

  public void setActualConvertedRate(BigDecimal actualConvertedRate) {
    this.actualConvertedRate = actualConvertedRate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description == null ? "" : description;
  }

  public String getTermsAndCondition() {
    return termsAndCondition;
  }

  public void setTermsAndCondition(String termsAndCondition) {
    this.termsAndCondition = termsAndCondition == null ? "" : termsAndCondition;
  }

  public String getIncluded() {
    return included;
  }

  public void setIncluded(String included) {
    this.included = included == null ? "" : included;
  }

  public String getExcluded() {
    return excluded;
  }

  public void setExcluded(String excluded) {
    this.excluded = excluded == null ? "" : excluded;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

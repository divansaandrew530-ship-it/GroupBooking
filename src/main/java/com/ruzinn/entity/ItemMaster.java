package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "item_master",
    indexes = {
      @Index(name = "idx_item_master_name", columnList = "item_name", unique = true),
      @Index(name = "idx_item_master_category", columnList = "category")
    })
public class ItemMaster {

  public static final String DATED_TYPE_DA = "DA";
  public static final String DATED_TYPE_DT = "DT";
  public static final String BOOKING_INFO_DFT = "DFT";
  public static final String BOOKING_INFO_ADD_ON = "ADD_ON";

  @Id
  @Column(name = "item_id", length = 32, nullable = false)
  private String itemId;

  @Column(name = "item_name", nullable = false, length = 255)
  private String itemName;

  @Column(name = "country", length = 8)
  private String country = "UAE";

  @Column(name = "dated_type", nullable = false, length = 8)
  private String datedType = DATED_TYPE_DA;

  @Column(name = "category", length = 80)
  private String category = "";

  @Column(name = "api_timeslot", nullable = false)
  private boolean apiTimeslot;

  @Column(name = "api_booking", nullable = false)
  private boolean apiBooking;

  @Column(name = "adult_rate", precision = 12, scale = 2)
  private BigDecimal adultRate;

  @Column(name = "child_rate", precision = 12, scale = 2)
  private BigDecimal childRate;

  @Column(name = "booking_info", nullable = false, length = 20)
  private String bookingInfo = BOOKING_INFO_DFT;

  @Column(name = "field1", length = 120)
  private String field1 = "";

  @Column(name = "field2", length = 120)
  private String field2 = "";

  @Column(name = "field3", length = 120)
  private String field3 = "";

  @Column(name = "field4", length = 120)
  private String field4 = "";

  @Column(name = "field5", length = 120)
  private String field5 = "";

  @Column(name = "field6", length = 120)
  private String field6 = "";

  @Column(name = "field7", length = 120)
  private String field7 = "";

  @Column(name = "field8", length = 120)
  private String field8 = "";

  @Column(name = "field9", length = 120)
  private String field9 = "";

  @Column(name = "field10", length = 120)
  private String field10 = "";

  @Column(name = "field11", length = 120)
  private String field11 = "";

  @Column(name = "field12", length = 120)
  private String field12 = "";

  @Column(name = "field13", length = 120)
  private String field13 = "";

  @Column(name = "field14", length = 120)
  private String field14 = "";

  @Column(name = "field15", length = 120)
  private String field15 = "";

  @Column(name = "description", columnDefinition = "text")
  private String description = "";

  @Column(name = "terms_and_condition", columnDefinition = "text")
  private String termsAndCondition = "";

  @Column(name = "included", columnDefinition = "text")
  private String included = "";

  @Column(name = "excluded", columnDefinition = "text")
  private String excluded = "";

  @Column(name = "image_pdf", length = 255)
  private String imagePdf = "";

  @Column(name = "image_ticket", length = 255)
  private String imageTicket = "";

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @Column(name = "created_by", length = 32)
  private String createdBy;

  @Column(name = "updated_date", nullable = false)
  private Instant updatedDate;

  @Column(name = "updated_by", length = 32)
  private String updatedBy;

  public List<String> extraFields() {
    List<String> fields = new ArrayList<>(15);
    fields.add(blank(field1));
    fields.add(blank(field2));
    fields.add(blank(field3));
    fields.add(blank(field4));
    fields.add(blank(field5));
    fields.add(blank(field6));
    fields.add(blank(field7));
    fields.add(blank(field8));
    fields.add(blank(field9));
    fields.add(blank(field10));
    fields.add(blank(field11));
    fields.add(blank(field12));
    fields.add(blank(field13));
    fields.add(blank(field14));
    fields.add(blank(field15));
    return fields;
  }

  public void setExtraFields(List<String> fields) {
    List<String> values = fields == null ? List.of() : fields;
    field1 = at(values, 0);
    field2 = at(values, 1);
    field3 = at(values, 2);
    field4 = at(values, 3);
    field5 = at(values, 4);
    field6 = at(values, 5);
    field7 = at(values, 6);
    field8 = at(values, 7);
    field9 = at(values, 8);
    field10 = at(values, 9);
    field11 = at(values, 10);
    field12 = at(values, 11);
    field13 = at(values, 12);
    field14 = at(values, 13);
    field15 = at(values, 14);
  }

  private static String at(List<String> values, int index) {
    if (index >= values.size() || values.get(index) == null) {
      return "";
    }
    return values.get(index).trim();
  }

  private static String blank(String value) {
    return value == null ? "" : value;
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

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getDatedType() {
    return datedType;
  }

  public void setDatedType(String datedType) {
    this.datedType = datedType;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isApiTimeslot() {
    return apiTimeslot;
  }

  public void setApiTimeslot(boolean apiTimeslot) {
    this.apiTimeslot = apiTimeslot;
  }

  public boolean isApiBooking() {
    return apiBooking;
  }

  public void setApiBooking(boolean apiBooking) {
    this.apiBooking = apiBooking;
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

  public String getBookingInfo() {
    return bookingInfo;
  }

  public void setBookingInfo(String bookingInfo) {
    this.bookingInfo = bookingInfo;
  }

  public String getField1() {
    return field1;
  }

  public void setField1(String field1) {
    this.field1 = field1;
  }

  public String getField2() {
    return field2;
  }

  public void setField2(String field2) {
    this.field2 = field2;
  }

  public String getField3() {
    return field3;
  }

  public void setField3(String field3) {
    this.field3 = field3;
  }

  public String getField4() {
    return field4;
  }

  public void setField4(String field4) {
    this.field4 = field4;
  }

  public String getField5() {
    return field5;
  }

  public void setField5(String field5) {
    this.field5 = field5;
  }

  public String getField6() {
    return field6;
  }

  public void setField6(String field6) {
    this.field6 = field6;
  }

  public String getField7() {
    return field7;
  }

  public void setField7(String field7) {
    this.field7 = field7;
  }

  public String getField8() {
    return field8;
  }

  public void setField8(String field8) {
    this.field8 = field8;
  }

  public String getField9() {
    return field9;
  }

  public void setField9(String field9) {
    this.field9 = field9;
  }

  public String getField10() {
    return field10;
  }

  public void setField10(String field10) {
    this.field10 = field10;
  }

  public String getField11() {
    return field11;
  }

  public void setField11(String field11) {
    this.field11 = field11;
  }

  public String getField12() {
    return field12;
  }

  public void setField12(String field12) {
    this.field12 = field12;
  }

  public String getField13() {
    return field13;
  }

  public void setField13(String field13) {
    this.field13 = field13;
  }

  public String getField14() {
    return field14;
  }

  public void setField14(String field14) {
    this.field14 = field14;
  }

  public String getField15() {
    return field15;
  }

  public void setField15(String field15) {
    this.field15 = field15;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTermsAndCondition() {
    return termsAndCondition;
  }

  public void setTermsAndCondition(String termsAndCondition) {
    this.termsAndCondition = termsAndCondition;
  }

  public String getIncluded() {
    return included;
  }

  public void setIncluded(String included) {
    this.included = included;
  }

  public String getExcluded() {
    return excluded;
  }

  public void setExcluded(String excluded) {
    this.excluded = excluded;
  }

  public String getImagePdf() {
    return imagePdf;
  }

  public void setImagePdf(String imagePdf) {
    this.imagePdf = imagePdf;
  }

  public String getImageTicket() {
    return imageTicket;
  }

  public void setImageTicket(String imageTicket) {
    this.imageTicket = imageTicket;
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

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }
}

package com.ruzinn.dto;

public class Experience {
  private String id;
  private String name;
  private Double adultPrice;
  private Double childPrice;
  private String category;
  private String city;
  private String country;
  private boolean dated;
  private String currency = "AED";
  private boolean priceOnRequest;
  private String kind;
  private String datedType;
  private boolean apiTimeSlot;
  private boolean apiBooking;
  private Integer starCategory;
  private String propertyType;
  private java.util.List<RoomRate> roomRates;
  private String bookingInfo;
  private java.util.List<String> passengerFields;

  public static final String BOOKING_INFO_DFT = "DFT";
  public static final String BOOKING_INFO_ADD_ON = "ADD_ON";
  public static final java.util.List<String> DFT_PASSENGER_FIELDS =
      java.util.List.of("firstName", "lastName", "phone", "email");

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getAdultPrice() {
    return adultPrice;
  }

  public void setAdultPrice(Double adultPrice) {
    this.adultPrice = adultPrice;
  }

  public Double getChildPrice() {
    return childPrice;
  }

  public void setChildPrice(Double childPrice) {
    this.childPrice = childPrice;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public boolean isDated() {
    return dated;
  }

  public void setDated(boolean dated) {
    this.dated = dated;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public boolean isPriceOnRequest() {
    return priceOnRequest;
  }

  public void setPriceOnRequest(boolean priceOnRequest) {
    this.priceOnRequest = priceOnRequest;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getDatedType() {
    return datedType;
  }

  public void setDatedType(String datedType) {
    this.datedType = datedType;
  }

  public boolean isApiTimeSlot() {
    return apiTimeSlot;
  }

  public void setApiTimeSlot(boolean apiTimeSlot) {
    this.apiTimeSlot = apiTimeSlot;
  }

  public boolean isApiBooking() {
    return apiBooking;
  }

  public void setApiBooking(boolean apiBooking) {
    this.apiBooking = apiBooking;
  }

  public Integer getStarCategory() {
    return starCategory;
  }

  public void setStarCategory(Integer starCategory) {
    this.starCategory = starCategory;
  }

  public String getPropertyType() {
    return propertyType;
  }

  public void setPropertyType(String propertyType) {
    this.propertyType = propertyType;
  }

  public java.util.List<RoomRate> getRoomRates() {
    return roomRates;
  }

  public void setRoomRates(java.util.List<RoomRate> roomRates) {
    this.roomRates = roomRates;
  }

  public String getBookingInfo() {
    return bookingInfo;
  }

  public void setBookingInfo(String bookingInfo) {
    this.bookingInfo = bookingInfo;
  }

  public boolean isAddOnPassengerFields() {
    return BOOKING_INFO_ADD_ON.equalsIgnoreCase(bookingInfo)
        || "ADDON".equalsIgnoreCase(bookingInfo);
  }

  public java.util.List<String> getPassengerFields() {
    return passengerFields;
  }

  public void setPassengerFields(java.util.List<String> passengerFields) {
    this.passengerFields = passengerFields;
  }

  public boolean requiresPassengerDetails() {
    return isAddOnPassengerFields();
  }

  public java.util.List<PassengerFieldDef> passengerFormFields() {
    if (!requiresPassengerDetails()) {
      return java.util.List.of();
    }
    java.util.List<String> keys =
        passengerFields == null || passengerFields.isEmpty()
            ? DFT_PASSENGER_FIELDS
            : passengerFields;
    java.util.List<PassengerFieldDef> fields = new java.util.ArrayList<>();
    for (String key : keys) {
      if (key != null && !key.isBlank()) {
        fields.add(PassengerFieldDef.fromKey(key));
      }
    }
    return fields;
  }

  public boolean requiresTimeSlot() {
    return "DT".equalsIgnoreCase(datedType);
  }

  public boolean hasChildRate() {
    return childPrice != null;
  }

  public Double roomRate(String roomType) {
    if (roomType == null || roomRates == null) {
      return null;
    }
    String wanted = roomType.trim();
    for (RoomRate rate : roomRates) {
      if (rate != null && rate.getName() != null && rate.getName().equalsIgnoreCase(wanted)) {
        return rate.getPrice();
      }
    }
    return null;
  }
}

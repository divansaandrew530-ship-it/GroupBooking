package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;

public class ParkQuoteLine {
  private String itemId;
  private String name;
  private String kind;
  private String category;
  private String city;
  private String visitDate;
  private int adults;
  private int children;
  private Double adultPrice;
  private Double childPrice;
  private double lineTotal;
  private boolean priceOnRequest;
  private String currency = "AED";
  private String checkOut;
  private int rooms;
  private String roomType;
  private String timeSlot;
  private String bookingInfo;
  private boolean requiresPassengerDetails;
  private java.util.List<PassengerFieldDef> passengerFields = java.util.List.of();
  private java.util.List<ParkPassenger> passengers = new java.util.ArrayList<>();
  private String description = "";
  private String termsAndCondition = "";
  private String included = "";
  private String excluded = "";
  private Double aedAdultPrice;
  private Double aedChildPrice;
  private double aedLineTotal;
  private Double actualAdultPrice;
  private Double actualChildPrice;
  private double actualLineTotal;
  private Double aedActualAdultPrice;
  private Double aedActualChildPrice;
  private double aedActualLineTotal;
  private double roeRate = 1;
  private double convertedRate;

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getVisitDate() {
    return visitDate;
  }

  public void setVisitDate(String visitDate) {
    this.visitDate = visitDate;
  }

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

  public double getLineTotal() {
    return lineTotal;
  }

  public void setLineTotal(double lineTotal) {
    this.lineTotal = lineTotal;
  }

  public boolean isPriceOnRequest() {
    return priceOnRequest;
  }

  public void setPriceOnRequest(boolean priceOnRequest) {
    this.priceOnRequest = priceOnRequest;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
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

  public String getBookingInfo() {
    return bookingInfo;
  }

  public void setBookingInfo(String bookingInfo) {
    this.bookingInfo = bookingInfo;
  }

  public boolean isRequiresPassengerDetails() {
    return requiresPassengerDetails;
  }

  public void setRequiresPassengerDetails(boolean requiresPassengerDetails) {
    this.requiresPassengerDetails = requiresPassengerDetails;
  }

  public java.util.List<PassengerFieldDef> getPassengerFields() {
    return passengerFields;
  }

  public void setPassengerFields(java.util.List<PassengerFieldDef> passengerFields) {
    this.passengerFields = passengerFields == null ? java.util.List.of() : passengerFields;
  }

  public java.util.List<ParkPassenger> getPassengers() {
    return passengers;
  }

  public void setPassengers(java.util.List<ParkPassenger> passengers) {
    this.passengers = passengers == null ? new java.util.ArrayList<>() : passengers;
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

  public Double getAedAdultPrice() {
    return aedAdultPrice;
  }

  public void setAedAdultPrice(Double aedAdultPrice) {
    this.aedAdultPrice = aedAdultPrice;
  }

  public Double getAedChildPrice() {
    return aedChildPrice;
  }

  public void setAedChildPrice(Double aedChildPrice) {
    this.aedChildPrice = aedChildPrice;
  }

  public double getAedLineTotal() {
    return aedLineTotal;
  }

  public void setAedLineTotal(double aedLineTotal) {
    this.aedLineTotal = aedLineTotal;
  }

  public Double getActualAdultPrice() {
    return actualAdultPrice;
  }

  public void setActualAdultPrice(Double actualAdultPrice) {
    this.actualAdultPrice = actualAdultPrice;
  }

  public Double getActualChildPrice() {
    return actualChildPrice;
  }

  public void setActualChildPrice(Double actualChildPrice) {
    this.actualChildPrice = actualChildPrice;
  }

  public double getActualLineTotal() {
    return actualLineTotal;
  }

  public void setActualLineTotal(double actualLineTotal) {
    this.actualLineTotal = actualLineTotal;
  }

  public Double getAedActualAdultPrice() {
    return aedActualAdultPrice;
  }

  public void setAedActualAdultPrice(Double aedActualAdultPrice) {
    this.aedActualAdultPrice = aedActualAdultPrice;
  }

  public Double getAedActualChildPrice() {
    return aedActualChildPrice;
  }

  public void setAedActualChildPrice(Double aedActualChildPrice) {
    this.aedActualChildPrice = aedActualChildPrice;
  }

  public double getAedActualLineTotal() {
    return aedActualLineTotal;
  }

  public void setAedActualLineTotal(double aedActualLineTotal) {
    this.aedActualLineTotal = aedActualLineTotal;
  }

  public double getRoeRate() {
    return roeRate;
  }

  public void setRoeRate(double roeRate) {
    this.roeRate = roeRate;
  }

  public double getConvertedRate() {
    return convertedRate;
  }

  public void setConvertedRate(double convertedRate) {
    this.convertedRate = convertedRate;
  }

  public ParkPassenger passengerAt(int index) {
    if (passengers == null || index < 0 || index >= passengers.size() || passengers.get(index) == null) {
      return new ParkPassenger();
    }
    return passengers.get(index);
  }

  public boolean isAddOnPassengerFields() {
    return "ADD_ON".equalsIgnoreCase(bookingInfo) || "ADDON".equalsIgnoreCase(bookingInfo);
  }

  public boolean isStay() {
    return "stay".equalsIgnoreCase(kind);
  }

  public boolean isPark() {
    return "experience".equalsIgnoreCase(kind);
  }

  public String getKindLabel() {
    if (isStay()) {
      return "Hotel";
    }
    if (isPark()) {
      return "Park";
    }
    if ("visa".equalsIgnoreCase(kind)) {
      return "Visa";
    }
    if ("transfer".equalsIgnoreCase(kind)) {
      return "Transfer";
    }
    return kind == null || kind.isBlank() ? "Item" : kind;
  }

  public String getDisplayVisitDate() {
    return formatDate(visitDate);
  }

  public String getDisplayCheckOut() {
    return formatDate(checkOut);
  }

  public Integer getNights() {
    if (!isStay() || visitDate == null || checkOut == null) {
      return null;
    }
    try {
      long nights =
          java.time.temporal.ChronoUnit.DAYS.between(
              java.time.LocalDate.parse(visitDate), java.time.LocalDate.parse(checkOut));
      return nights > 0 ? (int) nights : null;
    } catch (Exception ex) {
      return null;
    }
  }

  public String getGuestSummary() {
    return countLabel(adults, "Adult", "Adults") + ", " + countLabel(children, "Child", "Children");
  }

  public String getTicketSummary() {
    return countLabel(adults, "Adult ticket", "Adult tickets")
        + ", "
        + countLabel(children, "Child ticket", "Child tickets");
  }

  public static String formatDate(String iso) {
    if (iso == null || iso.isBlank()) {
      return "Date not set";
    }
    try {
      return BookingPresentation.formatTravel(java.time.LocalDate.parse(iso));
    } catch (Exception ex) {
      return iso;
    }
  }

  private static String countLabel(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }
}

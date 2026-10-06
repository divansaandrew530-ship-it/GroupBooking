package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public class ParkLineRequest {
  private String itemId;
  private String visitDate;
  private Integer adults;
  private Integer children;
  private String checkOut;
  private Integer rooms;
  private String roomType;
  private String timeSlot;
  private Double adultRate;
  private Double childRate;
  private String description;
  private String termsAndCondition;
  private String included;
  private String excluded;
  private List<ParkPassenger> passengers = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getVisitDate() {
    return visitDate;
  }

  public void setVisitDate(String visitDate) {
    this.visitDate = visitDate;
  }

  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
  }

  public Integer getRooms() {
    return rooms;
  }

  public void setRooms(Integer rooms) {
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

  public Double getAdultRate() {
    return adultRate;
  }

  public void setAdultRate(Double adultRate) {
    this.adultRate = adultRate;
  }

  public Double getChildRate() {
    return childRate;
  }

  public void setChildRate(Double childRate) {
    this.childRate = childRate;
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

  public List<ParkPassenger> getPassengers() {
    return passengers;
  }

  public void setPassengers(List<ParkPassenger> passengers) {
    this.passengers = passengers;
  }
}

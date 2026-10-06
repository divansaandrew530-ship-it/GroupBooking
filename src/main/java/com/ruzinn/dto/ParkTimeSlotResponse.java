package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public class ParkTimeSlotResponse {
  private String itemId;
  private String date;
  private String experienceName;
  private String datedType;
  private boolean apiTimeSlot;
  private String source;
  private List<ParkTimeSlot> slots = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getExperienceName() {
    return experienceName;
  }

  public void setExperienceName(String experienceName) {
    this.experienceName = experienceName;
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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public List<ParkTimeSlot> getSlots() {
    return slots;
  }

  public void setSlots(List<ParkTimeSlot> slots) {
    this.slots = slots;
  }
}

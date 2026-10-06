package com.ruzinn.dto;

public class ParkTimeSlot {
  private String id;
  private String label;
  private String startTime;
  private int remaining;
  private boolean available;

  public ParkTimeSlot() {}

  public ParkTimeSlot(String id, String label, String startTime, int remaining, boolean available) {
    this.id = id;
    this.label = label;
    this.startTime = startTime;
    this.remaining = remaining;
    this.available = available;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public int getRemaining() {
    return remaining;
  }

  public void setRemaining(int remaining) {
    this.remaining = remaining;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }
}

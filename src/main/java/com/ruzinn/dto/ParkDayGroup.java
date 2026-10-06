package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public class ParkDayGroup {
  private final String date;
  private final String displayDate;
  private final List<ParkQuoteLine> items = new ArrayList<>();

  public ParkDayGroup(String date, String displayDate) {
    this.date = date;
    this.displayDate = displayDate;
  }

  public String getDate() {
    return date;
  }

  public String getDisplayDate() {
    return displayDate;
  }

  public List<ParkQuoteLine> getItems() {
    return items;
  }
}

package com.ruzinn.dto;

public class ParkDayPlan {
  private String date;
  private String title = "";
  private String note = "";

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title == null ? "" : title;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note == null ? "" : note;
  }
}

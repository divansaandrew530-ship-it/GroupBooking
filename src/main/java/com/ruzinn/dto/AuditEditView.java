package com.ruzinn.dto;

public record AuditEditView(String what, String change, String when, String who, String from, String to) {

  public AuditEditView(String what, String change, String when, String who) {
    this(what, change, when, who, null, null);
  }
}

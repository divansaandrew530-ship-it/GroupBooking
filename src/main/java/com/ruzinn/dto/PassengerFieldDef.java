package com.ruzinn.dto;

import java.util.Locale;

public class PassengerFieldDef {
  private String key;
  private String label;
  private String inputType;

  public PassengerFieldDef() {}

  public PassengerFieldDef(String key, String label, String inputType) {
    this.key = key;
    this.label = label;
    this.inputType = inputType;
  }

  public static PassengerFieldDef fromKey(String key) {
    String normalized = key == null ? "" : key.trim();
    return switch (normalized) {
      case "firstName" -> new PassengerFieldDef("firstName", "First name", "text");
      case "lastName" -> new PassengerFieldDef("lastName", "Last name", "text");
      case "phone" -> new PassengerFieldDef("phone", "Phone", "tel");
      case "email" -> new PassengerFieldDef("email", "Email", "email");
      case "nationality" -> new PassengerFieldDef("nationality", "Nationality", "text");
      case "passport" -> new PassengerFieldDef("passport", "Passport number", "text");
      case "dateOfBirth", "dob" -> new PassengerFieldDef("dateOfBirth", "Date of birth", "date");
      case "age" -> new PassengerFieldDef("age", "Age", "number");
      case "gender" -> new PassengerFieldDef("gender", "Gender", "text");
      default -> new PassengerFieldDef(normalized, humanize(normalized), "text");
    };
  }

  private static String humanize(String key) {
    if (key == null || key.isBlank()) {
      return "Detail";
    }
    String spaced = key.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2");
    return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getInputType() {
    return inputType;
  }

  public void setInputType(String inputType) {
    this.inputType = inputType;
  }
}

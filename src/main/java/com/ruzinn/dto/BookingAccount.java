package com.ruzinn.dto;

public record BookingAccount(String username, String companyName, String companyId) {

  public static final String DIRECT_COMPANY_NAME = "Direct";
  public static final String DIRECT_COMPANY_ID = "DIRECT";

  public static BookingAccount direct(String username) {
    return new BookingAccount(blankToEmpty(username), DIRECT_COMPANY_NAME, DIRECT_COMPANY_ID);
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}

package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public record CompanyBrand(
    String companyId,
    String name,
    String address,
    String email,
    String phone,
    String website,
    String place,
    boolean hasLogo,
    String logoUrl) {

  public static CompanyBrand rathin() {
    return new CompanyBrand("1", "Rathin", "", "", "", "", "", false, "");
  }

  public String displayName() {
    return blankTo(name, "Rathin");
  }

  public String footerLine() {
    List<String> parts = new ArrayList<>();
    add(parts, displayName());
    add(parts, address);
    add(parts, place);
    add(parts, email);
    add(parts, phone);
    add(parts, website);
    return String.join("  ·  ", parts);
  }

  public String contactLine() {
    List<String> parts = new ArrayList<>();
    add(parts, address);
    add(parts, place);
    add(parts, email);
    add(parts, phone);
    add(parts, website);
    return String.join("  ·  ", parts);
  }

  private static void add(List<String> parts, String value) {
    if (value != null && !value.isBlank()) {
      parts.add(value.trim());
    }
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}

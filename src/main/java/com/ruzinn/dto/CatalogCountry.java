package com.ruzinn.dto;


public record CatalogCountry(String code, String name, String flag, String flagUrl, int itemCount) {

  public static CatalogCountry from(ItemCountry country, int itemCount) {
    return new CatalogCountry(country.code(), country.name(), country.flag(), country.flagUrl(), itemCount);
  }
}

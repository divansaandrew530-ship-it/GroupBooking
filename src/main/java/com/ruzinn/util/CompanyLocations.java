package com.ruzinn.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Countries and cities offered on company registration.
 *
 * <p>Every ISO country is offered (English names, A–Z). A few countries also have a fixed city list;
 * for those the city must be picked from it, for all others any city name is accepted.
 */
public final class CompanyLocations {

  /** All countries, English names in alphabetical order (e.g. "India", "United Arab Emirates"). */
  public static final List<String> COUNTRIES = allCountries();

  /** Countries with a fixed city list. Other countries take a typed city name. */
  public static final Map<String, List<String>> CITIES_BY_COUNTRY = cities();

  private static List<String> allCountries() {
    TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (String code : Locale.getISOCountries()) {
      String name = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
      if (!name.isBlank()) {
        names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private static Map<String, List<String>> cities() {
    Map<String, List<String>> cities = new LinkedHashMap<>();
    cities.put(
        "United Arab Emirates",
        List.of("Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Ras Al Khaimah", "Fujairah", "Umm Al Quwain"));
    cities.put("Saudi Arabia", List.of("Riyadh", "Jeddah", "Dammam", "Mecca", "Medina"));
    cities.put("Oman", List.of("Muscat", "Salalah", "Sohar"));
    cities.put("Qatar", List.of("Doha"));
    cities.put("Kuwait", List.of("Kuwait City"));
    cities.put("Bahrain", List.of("Manama"));
    cities.put("India", List.of("Mumbai", "Delhi", "Bangalore", "Chennai", "Hyderabad", "Kochi"));
    cities.put("United Kingdom", List.of("London", "Manchester", "Birmingham"));
    cities.put("United States", List.of("New York", "Los Angeles", "Chicago", "Houston", "Miami"));
    return Map.copyOf(cities);
  }

  /**
   * The country must be one of {@link #COUNTRIES}. The city is required; when the country has a
   * fixed city list it must be one of those.
   */
  public static void requireCountryAndCity(String country, String city) {
    if (country == null || !COUNTRIES.contains(country)) {
      throw new IllegalArgumentException("Select a valid country");
    }
    if (city == null || city.isBlank()) {
      throw new IllegalArgumentException("Select a valid city");
    }
    List<String> cities = CITIES_BY_COUNTRY.get(country);
    if (cities != null && !cities.contains(city)) {
      throw new IllegalArgumentException("Select a valid city");
    }
  }

  private CompanyLocations() {}
}

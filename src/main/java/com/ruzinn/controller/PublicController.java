package com.ruzinn.controller;

import com.ruzinn.util.CompanyLocations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Data anyone may read without signing in (used by the registration form). */
@RestController
@RequestMapping("/api/public")
public class PublicController {

  /**
   * Countries and cities offered on company registration:
   * {@code {"countries": [all countries A–Z], "citiesByCountry": {"India": ["Mumbai", ...], ...}}}.
   * Only countries with a fixed city list appear in {@code citiesByCountry}; for the others the
   * city is typed in.
   */
  @GetMapping("/locations")
  public Map<String, Object> locations() {
    Map<String, List<String>> citiesByCountry = new LinkedHashMap<>();
    for (String country : CompanyLocations.COUNTRIES) {
      List<String> cities = CompanyLocations.CITIES_BY_COUNTRY.get(country);
      if (cities != null) {
        citiesByCountry.put(country, cities);
      }
    }
    return Map.of("countries", CompanyLocations.COUNTRIES, "citiesByCountry", citiesByCountry);
  }
}

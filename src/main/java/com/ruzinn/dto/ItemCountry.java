package com.ruzinn.dto;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record ItemCountry(String code, String name, String flag) {

  public static final String UAE = "UAE";
  public static final String IND = "IND";
  public static final String SIN = "SIN";

  public static final List<ItemCountry> OPTIONS =
      List.of(
          new ItemCountry(UAE, "United Arab Emirates", "ae"),
          new ItemCountry(IND, "India", "in"),
          new ItemCountry(SIN, "Singapore", "sg"));

  public static String normalize(String raw) {
    String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('.', ' ');
    value = value.replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
    if (value.isBlank()) {
      return UAE;
    }
    return switch (value) {
      case UAE, "AE", "ARE", "UNITED ARAB EMIRATES", "U.A.E", "U A E", "EMIRATES", "DUBAI", "ABU DHABI" ->
          UAE;
      case IND, "IN", "INDIA" -> IND;
      case SIN, "SG", "SGP", "SINGAPORE" -> SIN;
      default -> {
        for (ItemCountry option : OPTIONS) {
          if (option.code.equals(value) || option.name.toUpperCase(Locale.ROOT).equals(value)) {
            yield option.code;
          }
        }
        throw new IllegalArgumentException("Country must be UAE, India, or Singapore");
      }
    };
  }

  public static ItemCountry require(String raw) {
    return of(normalize(raw));
  }

  public static ItemCountry of(String code) {
    String normalized = normalize(code);
    return OPTIONS.stream()
        .filter(option -> option.code.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown country"));
  }

  public static Optional<ItemCountry> find(String code) {
    try {
      return Optional.of(of(code));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  public String flagUrl() {
    return "/images/flags/" + flag + ".svg";
  }
}

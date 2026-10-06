package com.ruzinn.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One guest on a booked item. The standard fields have their own properties; every other passenger
 * field an item asks for (passport, dateOfBirth, age, ...) is kept in {@link #getExtra()}.
 *
 * <p>JSON is flat: {@code {"firstName": "Ravi", "passport": "P123", "age": "36"}}. The checkout
 * also sends the same values under {@code "values"}; both forms are accepted.
 */
public class ParkPassenger {
  private String firstName;
  private String lastName;
  private String type;
  private String phone;
  private String email;

  /** Written as flat JSON properties (see {@link #extraFields()}), not as an "extra" object. */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Map<String, String> extra = new LinkedHashMap<>();

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Map<String, String> getExtra() {
    return extra;
  }

  public void setExtra(Map<String, String> extra) {
    this.extra = extra == null ? new LinkedHashMap<>() : extra;
  }

  /** The checkout's {@code "values": {fieldKey: value}} copy of the passenger fields. */
  @JsonProperty("values")
  public void setValues(Map<String, Object> values) {
    if (values == null) {
      return;
    }
    values.forEach((key, value) -> setValue(key, value == null ? null : String.valueOf(value)));
  }

  /** Any other JSON property is a passenger field (passport, dateOfBirth, age, ...). */
  @JsonAnySetter
  public void setAnyField(String key, Object value) {
    if ("label".equals(key) || value instanceof Map || value instanceof Iterable) {
      return; // display label / nested structures are not passenger data
    }
    setValue(key, value == null ? null : String.valueOf(value));
  }

  @JsonAnyGetter
  public Map<String, String> extraFields() {
    return extra;
  }

  public String getFullName() {
    String first = firstName == null ? "" : firstName.trim();
    String last = lastName == null ? "" : lastName.trim();
    return (first + " " + last).trim();
  }

  public String valueFor(String key) {
    if (key == null) {
      return null;
    }
    return switch (key) {
      case "firstName" -> firstName;
      case "lastName" -> lastName;
      case "type" -> type;
      case "phone" -> phone;
      case "email" -> email;
      default -> extra == null ? null : extra.get(key);
    };
  }

  public void setValue(String key, String value) {
    if (key == null || key.isBlank()) {
      return;
    }
    String trimmed = value == null ? null : value.trim();
    switch (key) {
      case "firstName" -> firstName = trimmed;
      case "lastName" -> lastName = trimmed;
      case "type" -> type = trimmed;
      case "phone" -> phone = trimmed;
      case "email" -> email = trimmed;
      case "fullName" -> {
        // computed from first/last name; ignore the echoed value
      }
      default -> {
        if (extra == null) {
          extra = new LinkedHashMap<>();
        }
        extra.put(key, trimmed);
      }
    }
  }
}

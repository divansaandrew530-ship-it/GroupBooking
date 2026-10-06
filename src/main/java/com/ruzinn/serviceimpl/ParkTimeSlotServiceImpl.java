package com.ruzinn.serviceimpl;

import com.ruzinn.dto.Experience;
import com.ruzinn.dto.ParkTimeSlot;
import com.ruzinn.dto.ParkTimeSlotResponse;
import com.ruzinn.service.ParkCatalogService;
import com.ruzinn.service.ParkTimeSlotService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParkTimeSlotServiceImpl implements ParkTimeSlotService {

  private static final Logger log = LoggerFactory.getLogger(ParkTimeSlotServiceImpl.class);

  private final ParkCatalogService catalogService;

  public ParkTimeSlotServiceImpl(ParkCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @Override
  public ParkTimeSlotResponse slotsFor(String itemId, String dateRaw) {
    if (itemId == null || itemId.isBlank()) {
      throw new IllegalArgumentException("Experience is required");
    }
    Experience experience = catalogService.require(itemId.trim());
    if (!experience.requiresTimeSlot()) {
      throw new IllegalArgumentException(experience.getName() + " does not use dated timeslots.");
    }
    LocalDate date = parseDate(dateRaw, experience.getName());
    ParkTimeSlotResponse response = new ParkTimeSlotResponse();
    response.setItemId(experience.getId());
    response.setDate(date.toString());
    response.setExperienceName(experience.getName());
    response.setDatedType(experience.getDatedType());
    response.setApiTimeSlot(experience.isApiTimeSlot());
    if (experience.isApiTimeSlot()) {
      response.setSource("api");
      response.setSlots(liveApiSlots(experience, date));
    } else {
      response.setSource("static");
      response.setSlots(staticSlots(experience, date));
    }
    log.debug(
        "Time slots for {} on {}: {} slot(s) from {}",
        experience.getId(), date, response.getSlots().size(), response.getSource());
    return response;
  }

  /**
   * Stand-in for supplier timeslot APIs (Burj Khalifa, Museum of the Future, Burj Al Arab).
   * Replace with the real HTTP client once credentials and endpoints are available.
   */
  private List<ParkTimeSlot> liveApiSlots(Experience experience, LocalDate date) {
    String key = experience.getName().toLowerCase(Locale.ROOT);
    if (key.contains("museum of the future")) {
      return List.of(
          slot("10:00", remaining(date, 18), true),
          slot("12:00", remaining(date, 14), true),
          slot("14:00", remaining(date, 11), date.getDayOfWeek().getValue() != 5),
          slot("16:00", remaining(date, 9), true),
          slot("18:00", remaining(date, 6), true));
    }
    if (key.contains("burj khalifa")) {
      return List.of(
          slot("10:00", remaining(date, 22), true),
          slot("11:30", remaining(date, 16), true),
          slot("13:00", remaining(date, 12), true),
          slot("15:00", remaining(date, 8), date.getDayOfWeek().getValue() < 6),
          slot("17:00", remaining(date, 10), true),
          slot("19:00", remaining(date, 7), true),
          slot("21:00", remaining(date, 5), true));
    }
    if (key.contains("burj al arab")) {
      return List.of(
          slot("09:30", remaining(date, 8), true),
          slot("11:00", remaining(date, 6), true),
          slot("14:00", remaining(date, 5), true),
          slot("16:30", remaining(date, 4), date.getDayOfWeek().getValue() != 7));
    }
    return staticSlots(experience, date);
  }

  private List<ParkTimeSlot> staticSlots(Experience experience, LocalDate date) {
    String key = experience.getName().toLowerCase(Locale.ROOT);
    List<String> times;
    if (key.contains("cruise") || key.contains("dhow")) {
      times = List.of("12:30", "15:30", "19:30");
    } else if (key.contains("desert") || key.contains("safari")) {
      times = List.of("15:00", "15:30", "16:00");
    } else if (key.contains("sky views") || key.contains("edge walk") || key.contains("glass slide")) {
      times = List.of("10:00", "12:00", "14:00", "16:00", "18:00", "20:00");
    } else if (key.contains("transfer") || key.contains("airport")) {
      times = List.of("00:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00");
    } else {
      times = List.of("09:00", "11:00", "13:00", "15:00", "17:00");
    }
    List<ParkTimeSlot> slots = new ArrayList<>();
    for (int i = 0; i < times.size(); i++) {
      boolean available = !(i == times.size() - 1 && date.getDayOfWeek().getValue() == 5);
      slots.add(slot(times.get(i), remaining(date, 20 - i * 2), available));
    }
    return slots;
  }

  private static ParkTimeSlot slot(String startTime, int remaining, boolean available) {
    int seats = Math.max(0, remaining);
    return new ParkTimeSlot(
        startTime.replace(":", ""),
        startTime,
        startTime,
        seats,
        available && seats > 0);
  }

  private static int remaining(LocalDate date, int base) {
    int wobble = Math.abs(date.getDayOfYear() % 7) - 2;
    return Math.max(0, base - wobble);
  }

  private static LocalDate parseDate(String dateRaw, String experienceName) {
    if (dateRaw == null || dateRaw.isBlank()) {
      throw new IllegalArgumentException("Visit date required for " + experienceName);
    }
    try {
      return LocalDate.parse(dateRaw.trim());
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Invalid visit date for " + experienceName);
    }
  }
}

package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SelectedItinerary {
  private final List<ParkQuoteLine> stays = new ArrayList<>();
  private final List<ParkDayGroup> parkDays = new ArrayList<>();
  private final List<ParkQuoteLine> extras = new ArrayList<>();

  public static SelectedItinerary from(ParkQuote quote) {
    SelectedItinerary view = new SelectedItinerary();
    if (quote == null || quote.getItems() == null) {
      return view;
    }
    Map<String, ParkDayGroup> days = new LinkedHashMap<>();
    for (ParkQuoteLine line : quote.getItems()) {
      if (line == null) {
        continue;
      }
      if (line.isStay()) {
        view.stays.add(line);
      } else if (line.isPark()) {
        String key = line.getVisitDate() == null || line.getVisitDate().isBlank() ? "" : line.getVisitDate();
        ParkDayGroup group =
            days.computeIfAbsent(key, date -> new ParkDayGroup(date, ParkQuoteLine.formatDate(date)));
        group.getItems().add(line);
      } else {
        view.extras.add(line);
      }
    }
    view.parkDays.addAll(days.values());
    return view;
  }

  public List<ParkQuoteLine> getStays() {
    return stays;
  }

  public List<ParkDayGroup> getParkDays() {
    return parkDays;
  }

  public List<ParkQuoteLine> getExtras() {
    return extras;
  }
}

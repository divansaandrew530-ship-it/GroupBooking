package com.ruzinn.service;

import com.ruzinn.dto.ParkTimeSlotResponse;

public interface ParkTimeSlotService {

  ParkTimeSlotResponse slotsFor(String itemId, String dateRaw);
}

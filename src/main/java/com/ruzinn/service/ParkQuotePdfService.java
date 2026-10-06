package com.ruzinn.service;

import com.ruzinn.dto.ParkQuote;

public interface ParkQuotePdfService {

  byte[] render(ParkQuote quote);
}

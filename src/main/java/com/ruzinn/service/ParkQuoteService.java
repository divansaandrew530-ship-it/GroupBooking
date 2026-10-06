package com.ruzinn.service;

import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;

public interface ParkQuoteService {

  ParkQuote quote(ParkQuoteRequest request);
}

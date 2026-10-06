package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkConfirmRequest;
import com.ruzinn.dto.ParkConfirmationResponse;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;

/** Turns a priced itinerary into a paid, recorded booking. */
public interface BookingCheckoutService {

  /**
   * Full checkout: prices the itinerary, validates passengers and payment details, takes payment
   * (company credit or card) and records the booking.
   *
   * @param user the signed-in user, or {@code null} for a guest checkout
   */
  ParkConfirmationResponse checkout(ParkConfirmRequest request, AuthSession user);

  /** Records an already priced quote as a booking, charging company credit when paid that way. */
  ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String paymentMethod);
}

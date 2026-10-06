package com.ruzinn.service;

import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.SentBookingMail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.QuoteMain;
import java.util.List;

public interface BookingConfirmationMailService {

  /** Mail for a new checkout booking: received, waiting for approval. */
  void sendAfterCommit(ParkBooking booking, ParkQuoteRequest request, String paymentNote);

  /** Mail for a stored booking; says "confirmed" when its status is CONFIRMED. */
  void sendAfterCommit(BookingMain booking, QuoteMain quote);

  SentBookingMail send(ParkBooking booking, ParkQuoteRequest request, String paymentNote);

  SentBookingMail send(BookingMain booking, QuoteMain quote);

  List<SentBookingMail> sentMails();

  void clearSent();
}

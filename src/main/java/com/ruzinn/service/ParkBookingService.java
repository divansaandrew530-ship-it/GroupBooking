package com.ruzinn.service;

import com.ruzinn.dto.BookingDocument;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;

public interface ParkBookingService {

  ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request);

  ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request, String paymentNote);

  BookingMainView confirmAndRecord(ParkQuote quote, ParkQuoteRequest request);

  BookingMainView getRecord(String bookingRefNum);

  java.util.List<BookingMainView> listCompanyBookings(String companyId);

  java.util.List<BookingMainView> listAllBookings();

  BookingMainView updateStatus(String bookingRefNum, String status);

  BookingMainView updateItemStatus(String bookingRefNum, Long itemId, String status);

  BookingMainView updateItemFollowUp(String bookingRefNum, Long itemId, String followUp);

  BookingMainView storeDocument(
      String bookingRefNum, String kind, String fileName, String contentType, byte[] bytes);

  BookingMainView storeItemDocument(
      String bookingRefNum, Long itemId, String kind, String fileName, String contentType, byte[] bytes);

  BookingDocument loadDocument(String bookingRefNum, String kind);

  BookingDocument loadItemDocument(String bookingRefNum, Long itemId, String kind);

  java.util.Optional<BookingDocument> findPreferredDocument(String bookingRefNum);

  java.util.Optional<BookingDocument> findPreferredItemDocument(String bookingRefNum, Long itemId);
}

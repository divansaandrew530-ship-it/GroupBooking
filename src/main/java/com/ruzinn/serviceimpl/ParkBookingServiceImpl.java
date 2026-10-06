package com.ruzinn.serviceimpl;

import com.ruzinn.dto.BookingDocument;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.service.BookingConfirmationMailService;
import com.ruzinn.service.BookingRecordService;
import com.ruzinn.service.ParkBookingService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkBookingServiceImpl implements ParkBookingService {

  private static final Logger log = LoggerFactory.getLogger(ParkBookingServiceImpl.class);

  private final BookingRecordService bookingRecordService;
  private final BookingConfirmationMailService bookingMailService;

  public ParkBookingServiceImpl(
      BookingRecordService bookingRecordService, BookingConfirmationMailService bookingMailService) {
    this.bookingRecordService = bookingRecordService;
    this.bookingMailService = bookingMailService;
  }

  @Override
  @Transactional
  public ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request) {
    return confirm(quote, request, "Booking confirmed");
  }

  @Override
  @Transactional
  public ParkBooking confirm(ParkQuote quote, ParkQuoteRequest request, String paymentNote) {
    if (quote == null || quote.getItems() == null || quote.getItems().isEmpty()) {
      throw new IllegalArgumentException("Nothing to book");
    }
    if (request == null || request.getLeadName() == null || request.getLeadName().isBlank()) {
      throw new IllegalArgumentException("Lead guest name is required");
    }
    ParkBooking booking = new ParkBooking();
    booking.setBookingRef(("PRK" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase());
    booking.setConfirmationId(
        ("CNF" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase());
    // Saved as PENDING: staff confirm it later from the bookings list.
    booking.setStatus("pending");
    booking.setLeadName(request.getLeadName().trim());
    booking.setEmail(request.getEmail() == null ? "" : request.getEmail().trim());
    booking.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
    booking.setQuote(quote);
    bookingRecordService.saveConfirmed(booking.getBookingRef(), quote, request);
    bookingMailService.sendAfterCommit(booking, request, paymentNote);
    log.info(
        "Park booking {} (confirmation {}) recorded for {}; confirmation mail queued ({})",
        booking.getBookingRef(), booking.getConfirmationId(), booking.getLeadName(), paymentNote);
    return booking;
  }

  @Override
  @Transactional
  public BookingMainView confirmAndRecord(ParkQuote quote, ParkQuoteRequest request) {
    ParkBooking booking = confirm(quote, request);
    return bookingRecordService.get(booking.getBookingRef());
  }

  @Override
  @Transactional(readOnly = true)
  public BookingMainView getRecord(String bookingRefNum) {
    return bookingRecordService.get(bookingRefNum);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<BookingMainView> listCompanyBookings(String companyId) {
    return bookingRecordService.listByCompany(companyId);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<BookingMainView> listAllBookings() {
    return bookingRecordService.listAll();
  }

  @Override
  @Transactional
  public BookingMainView updateStatus(String bookingRefNum, String status) {
    return bookingRecordService.updateStatus(bookingRefNum, status);
  }

  @Override
  @Transactional
  public BookingMainView updateItemStatus(String bookingRefNum, Long itemId, String status) {
    return bookingRecordService.updateItemStatus(bookingRefNum, itemId, status);
  }

  @Override
  @Transactional
  public BookingMainView updateItemFollowUp(String bookingRefNum, Long itemId, String followUp) {
    return bookingRecordService.updateItemFollowUp(bookingRefNum, itemId, followUp);
  }

  @Override
  @Transactional
  public BookingMainView storeDocument(
      String bookingRefNum, String kind, String fileName, String contentType, byte[] bytes) {
    return bookingRecordService.storeDocument(bookingRefNum, kind, fileName, contentType, bytes);
  }

  @Override
  @Transactional
  public BookingMainView storeItemDocument(
      String bookingRefNum, Long itemId, String kind, String fileName, String contentType, byte[] bytes) {
    return bookingRecordService.storeItemDocument(bookingRefNum, itemId, kind, fileName, contentType, bytes);
  }

  @Override
  @Transactional(readOnly = true)
  public BookingDocument loadDocument(String bookingRefNum, String kind) {
    return bookingRecordService.loadDocument(bookingRefNum, kind);
  }

  @Override
  @Transactional(readOnly = true)
  public BookingDocument loadItemDocument(String bookingRefNum, Long itemId, String kind) {
    return bookingRecordService.loadItemDocument(bookingRefNum, itemId, kind);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Optional<BookingDocument> findPreferredDocument(String bookingRefNum) {
    return bookingRecordService.findPreferredDocument(bookingRefNum);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Optional<BookingDocument> findPreferredItemDocument(String bookingRefNum, Long itemId) {
    return bookingRecordService.findPreferredItemDocument(bookingRefNum, itemId);
  }
}

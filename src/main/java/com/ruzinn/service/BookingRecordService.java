package com.ruzinn.service;

import com.ruzinn.dto.BookingDocument;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.QuoteMain;
import java.util.List;

public interface BookingRecordService {

  BookingMain saveConfirmed(String bookingRefNum, ParkQuote quote, ParkQuoteRequest request);

  BookingMain saveFromQuote(QuoteMain quote);

  BookingMainView updateStatus(String bookingRefNum, String status);

  BookingMainView updateItemStatus(String bookingRefNum, Long itemId, String status);

  BookingMainView updateItemFollowUp(String bookingRefNum, Long itemId, String followUp);

  BookingMainView storeDocument(String bookingRefNum, String kind, String fileName, String contentType, byte[] bytes);

  BookingMainView storeItemDocument(
      String bookingRefNum, Long itemId, String kind, String fileName, String contentType, byte[] bytes);

  BookingDocument loadDocument(String bookingRefNum, String kind);

  BookingDocument loadItemDocument(String bookingRefNum, Long itemId, String kind);

  java.util.Optional<BookingDocument> findPreferredDocument(String bookingRefNum);

  java.util.Optional<BookingDocument> findPreferredItemDocument(String bookingRefNum, Long itemId);

  List<BookingMainView> listByCompany(String companyId);

  List<BookingMainView> listAll();

  BookingMainView get(String bookingRefNum);

  BookingMainView toView(BookingMain main);
}

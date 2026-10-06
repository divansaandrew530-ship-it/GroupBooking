package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteMainView;
import com.ruzinn.entity.QuoteMain;
import java.util.List;

public interface QuoteRecordService {

  QuoteMainView saveGenerated(ParkQuote quote, ParkQuoteRequest request, AuthSession auth);

  QuoteMainView markSent(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String sentTo);

  QuoteMainView updateStatus(String quoteRefNum, String status);

  /**
   * Checks that {@code auth} may book the quote and that it has no booking yet. Called before
   * payment is taken so a quote cannot be booked (and paid for) twice.
   */
  QuoteMain requireAcceptable(AuthSession auth, String quoteRefNum);

  /** Marks the quote ACCEPTED and links it to the booking created by a successful checkout. */
  QuoteMainView markAccepted(String quoteRefNum, String bookingRefNum);

  QuoteMainView updateFollowUp(String quoteRefNum, String followUp);

  List<QuoteMainView> listByCompany(String companyId);

  List<QuoteMainView> listVisibleTo(AuthSession auth);

  QuoteMainView requireVisible(AuthSession auth, String quoteRef);

  QuoteMain requireVisibleEntity(AuthSession auth, String quoteRef);

  List<QuoteMainView> listAll();

  QuoteMainView get(String quoteRefNum);

  QuoteMainView getForContentEdit(String quoteRefNum);

  QuoteMainView toView(QuoteMain main);
}

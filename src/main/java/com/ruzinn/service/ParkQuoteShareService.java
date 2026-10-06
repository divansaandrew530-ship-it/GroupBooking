package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteDocument;
import com.ruzinn.dto.SentQuoteMail;

public interface ParkQuoteShareService {

  ParkQuote quote(ParkQuoteRequest request, AuthSession auth);

  QuoteDocument document(ParkQuoteRequest request, AuthSession auth);

  SentQuoteMail send(ParkQuoteRequest request, AuthSession auth);

  void applyPrepared(ParkQuote quote, ParkQuoteRequest request, AuthSession auth);
}

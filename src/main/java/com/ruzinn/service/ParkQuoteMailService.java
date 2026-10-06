package com.ruzinn.service;

import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteDocument;
import com.ruzinn.dto.SentQuoteMail;
import java.util.List;

public interface ParkQuoteMailService {

  SentQuoteMail send(String to, QuoteDocument document);

  SentQuoteMail send(String to, QuoteDocument document, ParkQuoteRequest request);

  List<SentQuoteMail> sentMails();

  void clearSent();
}

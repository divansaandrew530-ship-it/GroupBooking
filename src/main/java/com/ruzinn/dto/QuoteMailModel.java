package com.ruzinn.dto;

import java.util.List;

public record QuoteMailModel(
    String logoUrl,
    String quoteNumber,
    String issuedOn,
    String expireDateLabel,
    String preparedBy,
    String companyName,
    String guestName,
    String guestEmail,
    String currency,
    List<MailLineView> lines,
    String subtotal,
    String markup,
    String total) {}

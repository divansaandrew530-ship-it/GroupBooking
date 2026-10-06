package com.ruzinn.dto;

import java.time.Instant;

public record SentQuoteMail(
    String quoteNumber,
    String to,
    String subject,
    String html,
    byte[] pdf,
    String fileName,
    Instant sentAt,
    String transport) {}

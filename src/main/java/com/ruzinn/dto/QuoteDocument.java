package com.ruzinn.dto;

public record QuoteDocument(ParkQuote quote, String html, byte[] pdf, String fileName) {}

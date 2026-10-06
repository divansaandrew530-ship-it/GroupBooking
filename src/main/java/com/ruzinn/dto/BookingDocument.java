package com.ruzinn.dto;

public record BookingDocument(String kind, String fileName, String contentType, byte[] bytes) {}

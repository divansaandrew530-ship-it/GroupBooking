package com.ruzinn.dto;

public record AccountDocument(String kind, String fileName, String contentType, byte[] bytes) {}

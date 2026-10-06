package com.ruzinn.dto;

/** Optional body for approving or rejecting a company. {@code creditLimit} is only used on approve. */
public record CompanyReviewRequest(String note, String creditLimit) {}

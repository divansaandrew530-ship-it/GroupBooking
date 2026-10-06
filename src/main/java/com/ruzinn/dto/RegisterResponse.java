package com.ruzinn.dto;

public record RegisterResponse(
    Long companyId, String userId, String email, String status, String otp) {}

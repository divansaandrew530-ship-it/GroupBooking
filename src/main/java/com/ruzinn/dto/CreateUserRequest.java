package com.ruzinn.dto;

public record CreateUserRequest(String userName, String email, String password, String role, String companyId) {}

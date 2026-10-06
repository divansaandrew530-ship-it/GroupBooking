package com.ruzinn.dto;

public record RegisterRequest(
    String companyName,
    String userName,
    String email,
    String phone,
    String password,
    String registrationNo,
    String address,
    String website,
    String country,
    String city,
    String zipCode,
    String ownerTelephone,
    String operationsStaffName,
    String operationsStaffNumber,
    String financeName,
    String financeMobile,
    String financeTelephone,
    String financeEmail) {}

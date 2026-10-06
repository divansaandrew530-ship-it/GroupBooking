package com.ruzinn.dto;

import java.util.List;

public record BookingPartyCompany(String companyId, String companyName, List<BookingPartyUser> users) {}

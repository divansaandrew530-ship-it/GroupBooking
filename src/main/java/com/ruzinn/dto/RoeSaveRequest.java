package com.ruzinn.dto;

import java.math.BigDecimal;

public record RoeSaveRequest(BigDecimal usdRate, BigDecimal inrRate) {}

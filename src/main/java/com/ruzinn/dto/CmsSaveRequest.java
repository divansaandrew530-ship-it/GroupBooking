package com.ruzinn.dto;

import java.util.Map;

public record CmsSaveRequest(String lang, Map<String, String> values) {}

package com.ruzinn.dto;

import com.ruzinn.service.CmsTranslationService;

public record CmsRow(String section, String key, String english, String value) {
  public static CmsRow from(CmsTranslationService.UiRow row) {
    return new CmsRow(row.group(), row.key(), row.english(), row.value());
  }
}

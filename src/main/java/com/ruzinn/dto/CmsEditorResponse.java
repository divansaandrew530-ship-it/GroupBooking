package com.ruzinn.dto;

import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.CmsTranslationService;
import java.util.List;

public record CmsEditorResponse(String lang, List<CmsRow> rows) {
  public static CmsEditorResponse of(AppLocale locale, List<CmsTranslationService.UiRow> rows) {
    return new CmsEditorResponse(locale.code(), rows.stream().map(CmsRow::from).toList());
  }
}

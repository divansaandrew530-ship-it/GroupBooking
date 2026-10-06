package com.ruzinn.controller;

import com.ruzinn.dto.CmsEditorResponse;
import com.ruzinn.dto.CmsSaveRequest;
import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.CmsTranslationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CmsController {

  private final CmsTranslationService translations;

  public CmsController(CmsTranslationService translations) {
    this.translations = translations;
  }

  @GetMapping("/api/admin/cms")
  public CmsEditorResponse editor(@RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = AppLocale.from(lang == null ? AppLocale.current().code() : lang);
    return CmsEditorResponse.of(locale, translations.uiRows(locale));
  }

  @PostMapping("/api/admin/cms")
  public CmsEditorResponse save(@RequestBody CmsSaveRequest request) {
    AppLocale locale = AppLocale.from(request == null ? null : request.lang());
    translations.saveUi(locale, request == null ? null : request.values());
    return CmsEditorResponse.of(locale, translations.uiRows(locale));
  }

}

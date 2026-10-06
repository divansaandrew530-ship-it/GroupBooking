package com.ruzinn.controller;

import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.CmsTranslationService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the flattened UI translation map as JSON. Public (outside the AuthInterceptor paths in
 * {@link com.ruzinn.config.WebConfig}) because the sign-in screen needs it before login.
 */
@RestController
public class I18nController {

  private final CmsTranslationService translations;

  public I18nController(CmsTranslationService translations) {
    this.translations = translations;
  }

  @GetMapping("/api/i18n")
  public Map<String, String> i18n(@RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = AppLocale.from(lang);
    return translations.uiMap(locale);
  }
}

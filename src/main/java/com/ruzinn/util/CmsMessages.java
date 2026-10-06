package com.ruzinn.util;

import com.ruzinn.service.CmsTranslationService;
import org.springframework.stereotype.Component;

@Component("cms")
public class CmsMessages {

  private final CmsTranslationService translations;

  public CmsMessages(CmsTranslationService translations) {
    this.translations = translations;
  }

  public String t(String key) {
    return translations.ui(key);
  }

  public String t(String key, Object arg0) {
    return format(translations.ui(key), arg0);
  }

  public String t(String key, Object arg0, Object arg1) {
    return format(format(translations.ui(key), arg0), arg1, 1);
  }

  private static String format(String text, Object arg) {
    return format(text, arg, 0);
  }

  private static String format(String text, Object arg, int index) {
    String value = arg == null ? "" : String.valueOf(arg);
    return text.replace("{" + index + "}", value);
  }
}

package com.ruzinn.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

public enum AppLocale {
  EN("en", "English", "English", false),
  HI("hi", "Hindi", "हिन्दी", false),
  RU("ru", "Russian", "Русский", false),
  AR("ar", "Arabic", "العربية", true);

  private final String code;
  private final String englishName;
  private final String nativeName;
  private final boolean rtl;

  AppLocale(String code, String englishName, String nativeName, boolean rtl) {
    this.code = code;
    this.englishName = englishName;
    this.nativeName = nativeName;
    this.rtl = rtl;
  }

  public String code() {
    return code;
  }

  public String englishName() {
    return englishName;
  }

  public String nativeName() {
    return nativeName;
  }

  public boolean rtl() {
    return rtl;
  }

  public boolean english() {
    return this == EN;
  }

  public Locale javaLocale() {
    return Locale.forLanguageTag(code);
  }

  public String htmlLang() {
    return code;
  }

  public String htmlDir() {
    return rtl ? "rtl" : "ltr";
  }

  public String flagCountry() {
    return switch (this) {
      case EN -> "gb";
      case HI -> "in";
      case RU -> "ru";
      case AR -> "ae";
    };
  }

  public static List<AppLocale> all() {
    return Arrays.asList(values());
  }

  public static AppLocale current() {
    return from(LocaleContextHolder.getLocale());
  }

  public static AppLocale from(Locale locale) {
    if (locale == null) {
      return EN;
    }
    return from(locale.getLanguage());
  }

  public static AppLocale from(String code) {
    if (code == null || code.isBlank()) {
      return EN;
    }
    String normalized = code.trim().toLowerCase(Locale.ROOT);
    int dash = normalized.indexOf('-');
    if (dash > 0) {
      normalized = normalized.substring(0, dash);
    }
    for (AppLocale locale : values()) {
      if (locale.code.equals(normalized)) {
        return locale;
      }
    }
    return EN;
  }
}

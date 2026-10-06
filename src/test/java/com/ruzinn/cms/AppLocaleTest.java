package com.ruzinn.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ruzinn.enums.AppLocale;
import org.junit.jupiter.api.Test;

class AppLocaleTest {

  @Test
  void localesMapToNationalFlags() {
    assertEquals("gb", AppLocale.EN.flagCountry());
    assertEquals("in", AppLocale.HI.flagCountry());
    assertEquals("ru", AppLocale.RU.flagCountry());
    assertEquals("ae", AppLocale.AR.flagCountry());
  }
}

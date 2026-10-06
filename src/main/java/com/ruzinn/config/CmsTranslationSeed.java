package com.ruzinn.config;

import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.service.CmsTranslationService;
import com.ruzinn.service.LoginHomeService;
import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class CmsTranslationSeed {

  private final LoginHomeService loginHomeService;
  private final CmsTranslationService translations;

  public CmsTranslationSeed(LoginHomeService loginHomeService, CmsTranslationService translations) {
    this.loginHomeService = loginHomeService;
    this.translations = translations;
  }

  @PostConstruct
  public void seed() {
    LoginHomePage page = loginHomeService.ensureDefaults();
    translations.seedDefaults(page);
  }
}

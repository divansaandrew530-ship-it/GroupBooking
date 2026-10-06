package com.ruzinn.config;

import com.ruzinn.service.LoginHomeService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class LoginHomeSeed {

  private final LoginHomeService loginHomeService;

  public LoginHomeSeed(LoginHomeService loginHomeService) {
    this.loginHomeService = loginHomeService;
  }

  @PostConstruct
  public void seed() {
    loginHomeService.ensureDefaults();
  }
}

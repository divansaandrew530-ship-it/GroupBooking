package com.ruzinn.config;

import com.ruzinn.service.AccountService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class AccountSeed {

  private final AccountService accountService;

  public AccountSeed(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostConstruct
  public void seed() {
    accountService.seedSuperAdmin();
  }
}

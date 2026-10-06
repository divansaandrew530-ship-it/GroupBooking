package com.ruzinn.config;

import com.ruzinn.service.RoeService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class RoeSeed {

  private final RoeService roeService;

  public RoeSeed(RoeService roeService) {
    this.roeService = roeService;
  }

  @PostConstruct
  public void seed() {
    roeService.seedDefaults();
  }
}

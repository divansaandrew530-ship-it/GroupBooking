package com.ruzinn.controller;

import com.ruzinn.dto.RoeSaveRequest;
import com.ruzinn.service.RoeService;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoeController {

  private final RoeService roeService;

  public RoeController(RoeService roeService) {
    this.roeService = roeService;
  }

  @PostMapping("/api/admin/roe")
  public Map<String, Object> save(@RequestBody RoeSaveRequest request, HttpSession session) {
    roeService.updateMaster(
        request == null ? null : request.usdRate(), request == null ? null : request.inrRate(), SessionUser.current(session));
    return Map.of("base", RoeService.AED, "rates", roeService.quoteRates());
  }

}

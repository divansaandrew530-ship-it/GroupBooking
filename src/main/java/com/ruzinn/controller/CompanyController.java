package com.ruzinn.controller;

import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.CompanyReviewRequest;
import com.ruzinn.dto.CompanyView;
import com.ruzinn.dto.CompanyWithUsers;
import com.ruzinn.service.CompanyService;
import com.ruzinn.util.FileResponses;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Companies: Super Admin review (approve/reject), registration documents and logos. */
@RestController
public class CompanyController {

  private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

  private final CompanyService companyService;

  public CompanyController(CompanyService companyService) {
    this.companyService = companyService;
  }

  @GetMapping("/api/admin/companies")
  public List<CompanyView> companies(HttpSession session) {
    return companyService.listCompanies(SessionUser.current(session));
  }

  /** Every company together with its users. */
  @GetMapping("/api/admin/companies/directory")
  public List<CompanyWithUsers> directory(HttpSession session) {
    return companyService.listCompaniesWithUsers(SessionUser.current(session));
  }

  /** Body (optional): {@code {"note": "...", "creditLimit": "500"}}. */
  @PostMapping("/api/admin/companies/{companyId}/approve")
  public CompanyView approve(
      @PathVariable String companyId,
      @RequestBody(required = false) CompanyReviewRequest body,
      HttpSession session) {
    AuthSession user = SessionUser.current(session);
    log.info("POST approve company {} by {}", companyId, user == null ? "anonymous" : user.email());
    String note = body == null ? null : body.note();
    BigDecimal creditLimit = body == null ? null : toCreditLimit(body.creditLimit());
    return companyService.approve(user, companyId, note, creditLimit);
  }

  /** Body (optional): {@code {"note": "..."}}. */
  @PostMapping("/api/admin/companies/{companyId}/reject")
  public CompanyView reject(
      @PathVariable String companyId,
      @RequestBody(required = false) CompanyReviewRequest body,
      HttpSession session) {
    AuthSession user = SessionUser.current(session);
    log.info("POST reject company {} by {}", companyId, user == null ? "anonymous" : user.email());
    String note = body == null ? null : body.note();
    return companyService.reject(user, companyId, note);
  }

  @PostMapping(value = "/api/admin/companies/{companyId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CompanyView uploadLogo(
      @PathVariable String companyId, @RequestParam("logo") MultipartFile logo, HttpSession session) {
    return companyService.saveLogo(SessionUser.current(session), companyId, logo);
  }

  /** Registration document download; {@code kind} is {@code vat} or {@code licence}. */
  @GetMapping("/admin/companies/{companyId}/documents/{kind}")
  public ResponseEntity<byte[]> downloadDocument(
      @PathVariable String companyId, @PathVariable String kind, HttpSession session) {
    Optional<AccountDocument> document = companyService.findDocument(SessionUser.current(session), companyId, kind);
    if (document.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    AccountDocument file = document.get();
    return FileResponses.download(file.fileName(), file.contentType(), file.bytes());
  }

  /** Company logo image, used in quotes and PDFs. */
  @GetMapping("/companies/{companyId}/logo")
  public ResponseEntity<byte[]> logo(@PathVariable String companyId, HttpSession session) {
    Optional<AccountDocument> logo = companyService.findLogo(SessionUser.current(session), companyId);
    if (logo.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return FileResponses.image(logo.get().contentType(), logo.get().bytes());
  }

  private static BigDecimal toCreditLimit(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Credit limit must be a number");
    }
  }
}

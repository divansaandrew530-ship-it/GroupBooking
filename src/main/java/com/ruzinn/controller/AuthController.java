package com.ruzinn.controller;

import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.RegisterRequest;
import com.ruzinn.dto.RegisterResponse;
import com.ruzinn.service.AccountService;
import com.ruzinn.util.FileResponses;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Sign-in and company registration.
 *
 * <p>Registration flow: {@code POST /register} returns an OTP (sent by email), then {@code POST
 * /verify-otp} signs the new user in. The company stays pending until a Super Admin approves it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final int REMEMBER_ME_SECONDS = 30 * 24 * 60 * 60;

  private final AccountService accountService;

  public AuthController(AccountService accountService) {
    this.accountService = accountService;
  }

  /** Quick registration with JSON (profile fields optional, no documents). */
  @PostMapping("/register")
  public RegisterResponse register(@RequestBody RegisterRequest request) {
    return accountService.register(request);
  }

  /**
   * Full registration as multipart form: profile fields required, VAT/licence/logo stored. The
   * Angular register page posts to {@code /register-full}; both paths do the same.
   */
  @PostMapping(
      value = {"/register", "/register-full"},
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RegisterResponse registerWithDocuments(
      @ModelAttribute RegisterRequest request,
      @RequestParam(required = false) MultipartFile vatCertificate,
      @RequestParam(required = false) MultipartFile licence,
      @RequestParam(required = false) MultipartFile logo) {
    return accountService.register(
        request, true, optionalDocument("vat", vatCertificate), optionalDocument("licence", licence), logo);
  }

  /** Body: {@code {"email": "...", "otp": "123456"}}. */
  @PostMapping("/verify-otp")
  public AuthSession verifyOtp(@RequestBody Map<String, String> body, HttpSession session) {
    AuthSession auth = accountService.verifyRegistration(body.get("email"), body.get("otp"));
    SessionUser.signIn(session, auth);
    return auth;
  }

  /** Body: {@code {"email": "...", "password": "...", "remember": "true"}} ({@code remember} optional). */
  @PostMapping("/login")
  public AuthSession login(@RequestBody Map<String, String> body, HttpSession session) {
    AuthSession auth = accountService.login(body.get("email"), body.get("password"));
    SessionUser.signIn(session, auth);
    if ("1".equals(body.get("remember")) || "true".equalsIgnoreCase(body.get("remember"))) {
      session.setMaxInactiveInterval(REMEMBER_ME_SECONDS);
    }
    return auth;
  }

  @PostMapping("/logout")
  public Map<String, String> logout(HttpSession session) {
    session.invalidate();
    return Map.of("status", "signed-out");
  }

  /**
   * The signed-in user. No sign-in required: a visitor who is not signed in gets {@code 204 No
   * Content} instead of a 401, so the app can check the session on every page load without errors.
   */
  @GetMapping("/me")
  public ResponseEntity<AuthSession> me(HttpSession session) {
    AuthSession user = SessionUser.current(session);
    return user == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(user);
  }

  private static AccountDocument optionalDocument(String kind, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    return new AccountDocument(kind, file.getOriginalFilename(), file.getContentType(), FileResponses.requireBytes(file));
  }
}

package com.ruzinn.serviceimpl;

import com.ruzinn.config.AuthProperties;
import com.ruzinn.entity.EmailOtp;
import com.ruzinn.repository.EmailOtpRepository;
import com.ruzinn.service.OtpService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpServiceImpl implements OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

  private static final SecureRandom RANDOM = new SecureRandom();

  private final EmailOtpRepository otpRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthProperties authProperties;

  public OtpServiceImpl(
      EmailOtpRepository otpRepository, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
    this.otpRepository = otpRepository;
    this.passwordEncoder = passwordEncoder;
    this.authProperties = authProperties;
  }

  @Override
  @Transactional
  public String issue(String email, String purpose) {
    String normalized = normalize(email);
    Instant now = Instant.now();
    for (EmailOtp existing :
        otpRepository.findByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(
            normalized, purpose)) {
      existing.setConsumedAt(now);
    }
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    EmailOtp otp = new EmailOtp();
    otp.setEmail(normalized);
    otp.setPurpose(purpose);
    otp.setCodeHash(passwordEncoder.encode(code));
    otp.setCreatedDate(now);
    otp.setExpiresAt(now.plus(authProperties.getOtpTtlMinutes(), ChronoUnit.MINUTES));
    otpRepository.save(otp);
    // Never log the code itself.
    log.info("OTP issued for {} ({}), valid until {}", normalized, purpose, otp.getExpiresAt());
    return code;
  }

  @Override
  @Transactional
  public void consume(String email, String purpose, String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("OTP is required");
    }
    String normalized = normalize(email);
    EmailOtp otp =
        otpRepository
            .findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(normalized, purpose)
            .orElse(null);
    if (otp == null) {
      log.warn("OTP check failed for {} ({}): no open OTP", normalized, purpose);
      throw new IllegalArgumentException("No OTP found for this email");
    }
    if (otp.getExpiresAt().isBefore(Instant.now())) {
      log.warn("OTP check failed for {} ({}): expired at {}", normalized, purpose, otp.getExpiresAt());
      throw new IllegalArgumentException("OTP has expired. Register again to get a new code.");
    }
    if (!passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
      log.warn("OTP check failed for {} ({}): wrong code", normalized, purpose);
      throw new IllegalArgumentException("Invalid OTP");
    }
    otp.setConsumedAt(Instant.now());
    log.info("OTP verified for {} ({})", normalized, purpose);
  }

  private static String normalize(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    return email.trim().toLowerCase();
  }
}

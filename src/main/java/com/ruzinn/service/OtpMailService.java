package com.ruzinn.service;

public interface OtpMailService {

  /** Emails the registration OTP. Without SMTP settings, or when sending fails, it is only logged. */
  void sendRegistrationOtp(String to, String code, int validMinutes);
}

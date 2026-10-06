package com.ruzinn.serviceimpl;

import com.ruzinn.config.MailProperties;
import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.service.MailService;
import com.ruzinn.service.OtpMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OtpMailServiceImpl implements OtpMailService {

  private static final Logger log = LoggerFactory.getLogger(OtpMailServiceImpl.class);

  private final MailProperties mailProperties;
  private final MailService mailService;

  public OtpMailServiceImpl(MailProperties mailProperties, MailService mailService) {
    this.mailProperties = mailProperties;
    this.mailService = mailService;
  }

  @Override
  public void sendRegistrationOtp(String to, String code, int validMinutes) {
    log.info("Registration OTP email requested for {} (valid {} min)", to, validMinutes);
    String subject = "Rathin verification code: " + code;
    String html =
        """
        <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#222">
          <img src="%s" alt="Rathin" style="height:48px;margin-bottom:16px">
          <p>Use this code to verify your email and complete your company registration:</p>
          <p style="font-size:28px;font-weight:bold;letter-spacing:6px;margin:24px 0">%s</p>
          <p>The code is valid for %d minutes. If you did not request it, you can ignore this email.</p>
        </div>
        """
            .formatted(mailProperties.logoUrl(), code, validMinutes);
    // Sent once the registration is committed: no mail for a registration that rolled back, and the
    // database transaction is not held open while the mail server responds.
    runAfterCommit(() -> deliver(OutgoingMail.html(to, subject, html)));
  }

  private void deliver(OutgoingMail mail) {
    try {
      mailService.send(mail);
      if (mailService.enabled()) {
        log.info("Registration OTP email sent to {}", mail.to());
      }
    } catch (Exception ex) {
      // Registration still succeeds; the OTP is saved and "Send OTP" issues a fresh one.
      // MailServiceImpl has already logged the provider error in full.
      log.error("Registration OTP email to {} FAILED: {}", mail.to(), ex.getMessage());
    }
  }

  private static void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }
}

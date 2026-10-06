package com.ruzinn.service;

import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.exception.MailDeliveryException;

/**
 * The single entry point for outgoing email. Routes to SMTP or Microsoft Graph according to
 * ruzinn.mail.provider; with no provider configured, messages are only logged.
 */
public interface MailService {

  /** True when messages are actually delivered (not just logged). */
  boolean enabled();

  /** "smtp", "graph", or "local" when mail is only logged. */
  String transportName();

  /**
   * Delivers the message, or only logs it when mail is not enabled.
   *
   * @throws MailDeliveryException when the provider refuses or fails to deliver it
   */
  void send(OutgoingMail mail);
}

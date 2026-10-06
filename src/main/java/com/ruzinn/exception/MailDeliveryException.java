package com.ruzinn.exception;

/** A mail provider (SMTP or Microsoft Graph) refused or failed to deliver a message. */
public class MailDeliveryException extends RuntimeException {

  public MailDeliveryException(String message) {
    super(message);
  }

  public MailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}

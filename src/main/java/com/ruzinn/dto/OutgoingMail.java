package com.ruzinn.dto;

/** One HTML email to a single recipient, with an optional file attachment. */
public record OutgoingMail(
    String to,
    String subject,
    String html,
    byte[] attachment,
    String attachmentName,
    String attachmentContentType) {

  public static OutgoingMail html(String to, String subject, String html) {
    return new OutgoingMail(to, subject, html, null, null, null);
  }

  public static OutgoingMail htmlWithAttachment(
      String to, String subject, String html, byte[] attachment, String name, String contentType) {
    return new OutgoingMail(to, subject, html, attachment, name, contentType);
  }

  public boolean hasAttachment() {
    return attachment != null && attachment.length > 0;
  }

  public String attachmentNameOrDefault() {
    return attachmentName == null || attachmentName.isBlank() ? "document.pdf" : attachmentName;
  }

  public String attachmentContentTypeOrDefault() {
    return attachmentContentType == null || attachmentContentType.isBlank()
        ? "application/pdf"
        : attachmentContentType;
  }
}

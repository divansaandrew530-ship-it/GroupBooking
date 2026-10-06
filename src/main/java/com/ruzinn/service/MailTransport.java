package com.ruzinn.service;

import com.ruzinn.dto.OutgoingMail;

/** Delivers one message through a concrete provider. Throws on any failure. */
public interface MailTransport {

  void send(OutgoingMail mail) throws Exception;
}

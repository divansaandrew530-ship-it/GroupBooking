package com.ruzinn.util;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.service.AccountService;
import jakarta.servlet.http.HttpSession;

/** Reads and writes the signed-in user kept in the HTTP session. */
public final class SessionUser {

  private SessionUser() {}

  /** The signed-in user, or {@code null} when nobody is signed in. */
  public static AuthSession current(HttpSession session) {
    Object value = session == null ? null : session.getAttribute(AccountService.SESSION_KEY);
    return value instanceof AuthSession auth ? auth : null;
  }

  public static void signIn(HttpSession session, AuthSession auth) {
    session.setAttribute(AccountService.SESSION_KEY, auth);
  }
}

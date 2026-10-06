package com.ruzinn.util;

import com.ruzinn.dto.AuthSession;

public final class AuditActor {

  public record Snapshot(
      String userId, String userName, String email, String role, String companyId, String companyName) {}

  private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

  private AuditActor() {}

  public static void bind(AuthSession auth) {
    CURRENT.set(from(auth));
  }

  public static void clear() {
    CURRENT.remove();
  }

  public static Snapshot current() {
    return CURRENT.get();
  }

  public static Snapshot from(AuthSession auth) {
    if (auth == null) {
      return null;
    }
    return new Snapshot(
        auth.userId(), auth.userName(), auth.email(), auth.role(), auth.companyId(), auth.companyName());
  }
}

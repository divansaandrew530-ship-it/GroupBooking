package com.ruzinn.service;

import com.ruzinn.dto.BookingSession;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class BookingSessionStore {

  private final Map<String, BookingSession> sessions = new ConcurrentHashMap<>();

  public BookingSession create(String module) {
    String id = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    BookingSession session = new BookingSession(id, module);
    sessions.put(id, session);
    return session;
  }

  public BookingSession get(String id) {
    return sessions.get(id);
  }

  public BookingSession require(String id) {
    BookingSession session = sessions.get(id);
    if (session == null) {
      throw new IllegalArgumentException("Session expired or not found: " + id);
    }
    return session;
  }
}

package com.ruzinn.dto;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BookingSession {

  private final String id;
  private final String module;
  private final Instant createdAt;
  private Instant updatedAt;
  private final Map<String, Object> data = new ConcurrentHashMap<>();

  public BookingSession(String id, String module) {
    this.id = id;
    this.module = module;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public String getId() {
    return id;
  }

  public String getModule() {
    return module;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void put(String key, Object value) {
    data.put(key, value);
    updatedAt = Instant.now();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    Object value = data.get(key);
    if (value == null) {
      return null;
    }
    return (T) value;
  }
}

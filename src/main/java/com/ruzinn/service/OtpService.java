package com.ruzinn.service;

public interface OtpService {

  String issue(String email, String purpose);

  void consume(String email, String purpose, String code);
}

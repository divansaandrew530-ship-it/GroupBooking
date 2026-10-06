package com.ruzinn.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookingNotFoundException extends RuntimeException {

  public BookingNotFoundException(String bookingRefNum) {
    super("Booking not found: " + bookingRefNum);
  }
}

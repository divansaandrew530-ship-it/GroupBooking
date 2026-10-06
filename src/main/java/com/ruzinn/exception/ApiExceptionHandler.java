package com.ruzinn.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

/** Turns service exceptions into {@code {"error": "..."}} responses and logs why each call failed. */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(BookingNotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(BookingNotFoundException ex, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(QuoteNotFoundException.class)
  public ResponseEntity<Map<String, String>> quoteNotFound(QuoteNotFoundException ex, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, String>> unauthorized(UnauthorizedException ex, HttpServletRequest request) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<Map<String, String>> uploadFailed(MultipartException ex, HttpServletRequest request) {
    Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
    String message =
        cause != null && cause.getClass().getName().contains("FileCountLimitExceededException")
            ? "Could not save because the form sent too many fields. Try again without replacing every image."
            : "Could not save the uploaded files. Check the file size and try again.";
    return error(HttpStatus.BAD_REQUEST, message, ex, request);
  }

  private static ResponseEntity<Map<String, String>> error(
      HttpStatus status, String message, Exception ex, HttpServletRequest request) {
    String text = message == null ? status.getReasonPhrase() : message;
    if (ex.getCause() != null) {
      // Wrapped technical failure (SQL, IO, ...): keep the stack trace.
      log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status.value(), text, ex);
    } else {
      log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status.value(), text);
    }
    return ResponseEntity.status(status).body(Map.of("error", text));
  }
}

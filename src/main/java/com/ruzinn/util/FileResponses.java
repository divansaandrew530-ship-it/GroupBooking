package com.ruzinn.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** Builds HTTP responses for stored files and reads uploaded files. */
public final class FileResponses {

  private FileResponses() {}

  /** A file the browser downloads (Content-Disposition: attachment). */
  public static ResponseEntity<byte[]> download(String fileName, String contentType, byte[] bytes) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
    return ResponseEntity.ok()
        .headers(headers)
        .contentType(mediaType(contentType, MediaType.APPLICATION_OCTET_STREAM))
        .body(bytes);
  }

  /** An image shown inline, e.g. in an {@code <img>} tag. Unknown types are served as JPEG. */
  public static ResponseEntity<byte[]> image(String contentType, byte[] bytes) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .contentType(mediaType(contentType, MediaType.IMAGE_JPEG))
        .body(bytes);
  }

  /** The uploaded bytes; rejects a missing or empty file with a 400 error. */
  public static byte[] requireBytes(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Choose a file to upload");
    }
    try {
      return file.getBytes();
    } catch (IOException ex) {
      throw new IllegalArgumentException("Could not read the uploaded file");
    }
  }

  private static MediaType mediaType(String contentType, MediaType fallback) {
    if (contentType == null || contentType.isBlank()) {
      return fallback;
    }
    try {
      return MediaType.parseMediaType(contentType);
    } catch (Exception ex) {
      return fallback;
    }
  }
}

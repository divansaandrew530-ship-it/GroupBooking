package com.ruzinn.service;

import com.ruzinn.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BookingDocumentStore {

  private final Path root;

  public BookingDocumentStore(UploadProperties properties) {
    String directory = properties.getDirectory() == null || properties.getDirectory().isBlank()
        ? "uploads"
        : properties.getDirectory().trim();
    this.root = Path.of(directory).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.root);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not create upload directory " + this.root, ex);
    }
  }

  public Path root() {
    return root;
  }

  public String save(String bookingRef, String parkKey, Long itemId, String kind, String fileName, byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Choose a file to upload");
    }
    String ref = sanitize(bookingRef, "booking");
    String park = sanitize(parkKey, "park");
    String item = itemId == null ? "booking" : "item-" + itemId;
    String storedName = sanitize(kind, "file") + "_" + sanitize(fileName, "document.pdf");
    Path relative = Path.of(ref, park, item, storedName);
    Path destination = root.resolve(relative).normalize();
    if (!destination.startsWith(root)) {
      throw new IllegalArgumentException("Invalid upload path");
    }
    try {
      Files.createDirectories(destination.getParent());
      Files.write(destination, bytes);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store the uploaded file", ex);
    }
    return root.relativize(destination).toString().replace('\\', '/');
  }

  public byte[] read(String relativePath) {
    Path file = resolve(relativePath);
    try {
      return Files.readAllBytes(file);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read uploaded file", ex);
    }
  }

  public boolean exists(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return false;
    }
    try {
      return Files.isRegularFile(resolve(relativePath));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public void deleteIfPresent(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(resolve(relativePath));
    } catch (IOException ignored) {
      // Best-effort cleanup when replacing a file.
    }
  }

  private Path resolve(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("File path is required");
    }
    Path destination = root.resolve(relativePath).normalize();
    if (!destination.startsWith(root) || !Files.isRegularFile(destination)) {
      throw new IllegalArgumentException("Uploaded file was not found");
    }
    return destination;
  }

  static String sanitize(String value, String fallback) {
    String name = value == null ? "" : value.trim();
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0) {
      name = name.substring(slash + 1);
    }
    name = name.replaceAll("[^A-Za-z0-9._-]", "_");
    if (name.isBlank()) {
      return fallback;
    }
    return name.toLowerCase(Locale.ROOT);
  }
}

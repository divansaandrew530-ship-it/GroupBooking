package com.ruzinn.service;

import com.ruzinn.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LoginHomeMediaStore {

  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/webp");

  private final Path root;

  public LoginHomeMediaStore(UploadProperties properties) {
    String directory =
        properties.getDirectory() == null || properties.getDirectory().isBlank()
            ? "uploads"
            : properties.getDirectory().trim();
    this.root = Path.of(directory).toAbsolutePath().normalize().resolve("cms/login-home");
    try {
      Files.createDirectories(this.root);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not create login home upload directory", ex);
    }
  }

  public String saveSlide(Long slideId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("Upload a JPG, PNG, or WebP image");
    }
    String original = file.getOriginalFilename() == null ? "slide.jpg" : file.getOriginalFilename();
    String ext = extension(original, contentType);
    String stored = "slide-" + slideId + ext;
    Path destination = root.resolve(stored).normalize();
    if (!destination.startsWith(root)) {
      throw new IllegalArgumentException("Invalid upload path");
    }
    try {
      Files.write(destination, file.getBytes());
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store the slide image", ex);
    }
    return "cms/login-home/" + stored;
  }

  public byte[] readUploaded(String relativePath) {
    Path file = resolve(relativePath);
    try {
      return Files.readAllBytes(file);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read slide image", ex);
    }
  }

  public boolean existsUploaded(String relativePath) {
    if (relativePath == null || relativePath.isBlank() || relativePath.startsWith("classpath:")) {
      return false;
    }
    try {
      return Files.isRegularFile(resolve(relativePath));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public byte[] readClasspath(String classpathLocation) {
    String location = classpathLocation;
    if (location.startsWith("classpath:")) {
      location = location.substring("classpath:".length());
    }
    if (!location.startsWith("static/")) {
      location = "static/" + location;
    }
    ClassPathResource resource = new ClassPathResource(location);
    try {
      return resource.getInputStream().readAllBytes();
    } catch (IOException ex) {
      throw new IllegalStateException("Default slide image was not found", ex);
    }
  }

  public boolean existsClasspath(String classpathLocation) {
    try {
      readClasspath(classpathLocation);
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private Path resolve(String relativePath) {
    String path = relativePath.replace('\\', '/');
    if (path.startsWith("cms/login-home/")) {
      path = path.substring("cms/login-home/".length());
    }
    Path destination = root.resolve(path).normalize();
    if (!destination.startsWith(root) || !Files.isRegularFile(destination)) {
      throw new IllegalArgumentException("Uploaded file was not found");
    }
    return destination;
  }

  static String extension(String fileName, String contentType) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png") || contentType.contains("png")) {
      return ".png";
    }
    if (lower.endsWith(".webp") || contentType.contains("webp")) {
      return ".webp";
    }
    return ".jpg";
  }
}

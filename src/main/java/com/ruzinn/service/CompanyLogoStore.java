package com.ruzinn.service;

import com.ruzinn.config.UploadProperties;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyLogoStore {

  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/jpg", "image/pjpeg", "image/png");

  private final Path root;
  private final Path companiesRoot;

  public CompanyLogoStore(UploadProperties properties) {
    String directory =
        properties.getDirectory() == null || properties.getDirectory().isBlank()
            ? "uploads"
            : properties.getDirectory().trim();
    this.root = Path.of(directory).toAbsolutePath().normalize();
    this.companiesRoot = this.root.resolve("companies");
    try {
      Files.createDirectories(this.companiesRoot);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not create company logo upload directory", ex);
    }
  }

  public String save(Long companyId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try {
      return save(companyId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read the company logo", ex);
    }
  }

  public String save(Long companyId, String originalName, String contentType, byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    if (companyId == null) {
      throw new IllegalArgumentException("Company ID is required");
    }
    String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(type)) {
      throw new IllegalArgumentException("Upload a JPG or PNG company logo");
    }
    String ext = extension(originalName, type);
    Path directory = companiesRoot.resolve(String.valueOf(companyId));
    Path destination = directory.resolve("logo" + ext).normalize();
    if (!destination.startsWith(companiesRoot)) {
      throw new IllegalArgumentException("Invalid upload path");
    }
    try {
      Files.createDirectories(directory);
      deleteLogos(directory);
      Files.write(destination, bytes);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store the company logo", ex);
    }
    return "companies/" + companyId + "/logo" + ext;
  }

  public byte[] read(String relativePath) {
    Path file = resolve(relativePath);
    try {
      return Files.readAllBytes(file);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read company logo", ex);
    }
  }

  public byte[] readIfPresent(String relativePath) {
    if (!exists(relativePath)) {
      return null;
    }
    return read(relativePath);
  }

  public boolean exists(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return false;
    }
    String path = relativePath.replace('\\', '/');
    if (!path.startsWith("companies/")) {
      return false;
    }
    try {
      return Files.isRegularFile(resolve(path));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public String contentType(String relativePath) {
    String path = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
    if (path.endsWith(".png")) {
      return MediaType.IMAGE_PNG_VALUE;
    }
    return MediaType.IMAGE_JPEG_VALUE;
  }

  private void deleteLogos(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "logo.*")) {
      for (Path path : stream) {
        Files.deleteIfExists(path);
      }
    }
  }

  private Path resolve(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("Logo path is required");
    }
    String path = relativePath.replace('\\', '/');
    Path destination = root.resolve(path).normalize();
    if (!destination.startsWith(companiesRoot) || !Files.isRegularFile(destination)) {
      throw new IllegalArgumentException("Uploaded logo was not found");
    }
    return destination;
  }

  static String extension(String fileName, String contentType) {
    String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png") || (contentType != null && contentType.contains("png"))) {
      return ".png";
    }
    return ".jpg";
  }
}

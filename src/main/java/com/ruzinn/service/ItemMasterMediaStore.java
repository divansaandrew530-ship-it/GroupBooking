package com.ruzinn.service;

import com.ruzinn.config.UploadProperties;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ItemMasterMediaStore {

  public static final String KIND_PDF = "pdf";
  public static final String KIND_TICKET = "ticket";

  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/webp");

  private final Path root;
  private final Path itemsRoot;

  public ItemMasterMediaStore(UploadProperties properties) {
    String directory =
        properties.getDirectory() == null || properties.getDirectory().isBlank()
            ? "uploads"
            : properties.getDirectory().trim();
    this.root = Path.of(directory).toAbsolutePath().normalize();
    this.itemsRoot = this.root.resolve("items");
    try {
      Files.createDirectories(this.itemsRoot);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not create item image upload directory", ex);
    }
  }

  public String save(String itemId, String kind, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("Upload a JPG, PNG, or WebP image for " + label(kind));
    }
    String id = sanitize(itemId);
    String kindKey = normalizeKind(kind);
    String original = file.getOriginalFilename() == null ? kindKey + ".jpg" : file.getOriginalFilename();
    String ext = extension(original, contentType);
    Path directory = itemsRoot.resolve(id);
    Path destination = directory.resolve(kindKey + ext).normalize();
    if (!destination.startsWith(itemsRoot)) {
      throw new IllegalArgumentException("Invalid upload path");
    }
    try {
      Files.createDirectories(directory);
      deleteKindFiles(directory, kindKey);
      Files.write(destination, file.getBytes());
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store the " + label(kind) + " image", ex);
    }
    return "items/" + id + "/" + kindKey + ext;
  }

  public byte[] read(String relativePath) {
    Path file = resolve(relativePath);
    try {
      return Files.readAllBytes(file);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read item image", ex);
    }
  }

  public boolean exists(String relativePath) {
    if (relativePath == null || relativePath.isBlank() || !relativePath.replace('\\', '/').startsWith("items/")) {
      return false;
    }
    try {
      return Files.isRegularFile(resolve(relativePath));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public String contentType(String relativePath) {
    String path = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
    if (path.endsWith(".png")) {
      return MediaType.IMAGE_PNG_VALUE;
    }
    if (path.endsWith(".webp")) {
      return "image/webp";
    }
    return MediaType.IMAGE_JPEG_VALUE;
  }

  public void deleteItem(String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return;
    }
    Path directory = itemsRoot.resolve(sanitize(itemId)).normalize();
    if (!directory.startsWith(itemsRoot) || !Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(directory)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ignored) {
                  // best-effort cleanup when deleting an item
                }
              });
    } catch (IOException ignored) {
      // best-effort cleanup when deleting an item
    }
  }

  private void deleteKindFiles(Path directory, String kindKey) throws IOException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, kindKey + ".*")) {
      for (Path path : stream) {
        Files.deleteIfExists(path);
      }
    }
  }

  private Path resolve(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("Image path is required");
    }
    String path = relativePath.replace('\\', '/');
    Path destination = root.resolve(path).normalize();
    if (!destination.startsWith(itemsRoot) || !Files.isRegularFile(destination)) {
      throw new IllegalArgumentException("Uploaded image was not found");
    }
    return destination;
  }

  public static String normalizeKind(String kind) {
    String value = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    if (KIND_PDF.equals(value) || "image_pdf".equals(value) || "pdf".equals(value)) {
      return KIND_PDF;
    }
    if (KIND_TICKET.equals(value) || "image_ticket".equals(value) || "ticket".equals(value)) {
      return KIND_TICKET;
    }
    throw new IllegalArgumentException("Image kind must be pdf or ticket");
  }

  static String label(String kind) {
    return KIND_TICKET.equals(normalizeKind(kind)) ? "Image in ticket" : "Image in PDF";
  }

  static String sanitize(String itemId) {
    String id = itemId == null ? "" : itemId.trim().toUpperCase(Locale.ROOT);
    id = id.replaceAll("[^A-Z0-9_-]", "");
    if (id.isBlank()) {
      throw new IllegalArgumentException("Item ID is required");
    }
    return id;
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

package com.ruzinn.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruzinn.config.UploadProperties;
import com.ruzinn.service.BookingDocumentStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BookingDocumentStoreTest {

  @TempDir Path tempDir;

  @Test
  void savesFileUnderBookingRefAndParkFolders() throws Exception {
    UploadProperties properties = new UploadProperties();
    properties.setDirectory(tempDir.toString());
    BookingDocumentStore store = new BookingDocumentStore(properties);

    String path =
        store.save("PRKABC123", "Dubai Frame", 42L, "ticket", "desert ticket.pdf", "hello".getBytes());

    assertThat(path).isEqualTo("prkabc123/dubai_frame/item-42/ticket_desert_ticket.pdf");
    assertThat(store.exists(path)).isTrue();
    assertThat(store.read(path)).isEqualTo("hello".getBytes());
    assertThat(Files.isRegularFile(tempDir.resolve(path))).isTrue();
  }
}

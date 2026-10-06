package com.ruzinn.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruzinn.config.UploadProperties;
import com.ruzinn.service.ItemMasterMediaStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ItemMasterMediaStoreTest {

  @TempDir Path tempDir;

  @Test
  void savesPdfAndTicketImagesUnderItemFolder() throws Exception {
    UploadProperties properties = new UploadProperties();
    properties.setDirectory(tempDir.toString());
    ItemMasterMediaStore store = new ItemMasterMediaStore(properties);

    MockMultipartFile pdf =
        new MockMultipartFile("imagePdfFile", "park.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2});
    MockMultipartFile ticket =
        new MockMultipartFile("imageTicketFile", "ticket.png", "image/png", new byte[] {1, 2, 3});

    String pdfPath = store.save("itmabc123xyz", "pdf", pdf);
    String ticketPath = store.save("itmabc123xyz", "ticket", ticket);

    assertThat(pdfPath).isEqualTo("items/ITMABC123XYZ/pdf.jpg");
    assertThat(ticketPath).isEqualTo("items/ITMABC123XYZ/ticket.png");
    assertThat(store.exists(pdfPath)).isTrue();
    assertThat(store.read(ticketPath)).isEqualTo(new byte[] {1, 2, 3});
    assertThat(Files.isRegularFile(tempDir.resolve(pdfPath))).isTrue();

    store.deleteItem("ITMABC123XYZ");
    assertThat(store.exists(pdfPath)).isFalse();
  }

  @Test
  void rejectsUnsupportedImageType() {
    UploadProperties properties = new UploadProperties();
    properties.setDirectory(tempDir.toString());
    ItemMasterMediaStore store = new ItemMasterMediaStore(properties);
    MockMultipartFile gif =
        new MockMultipartFile("imagePdfFile", "park.gif", "image/gif", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> store.save("ITM123", "pdf", gif))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JPG, PNG, or WebP");
  }
}

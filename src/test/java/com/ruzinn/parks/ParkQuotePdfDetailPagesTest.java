package com.ruzinn.parks;

import static com.ruzinn.TestApi.apiLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ruzinn.entity.ItemMaster;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.service.ItemMasterMediaStore;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ParkQuotePdfDetailPagesTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ItemMasterRepository items;
  @Autowired private ItemMasterMediaStore mediaStore;

  @Test
  void quotePdfKeepsQuotationAndAppendsParkAndHotelDetailPages() throws Exception {
    upsertMaster(
        "ITMTESTFRAME001",
        "Dubai Frame",
        "Fantasy",
        "QUOTEPDF-PARK-DESC-DUBAI-FRAME",
        "QUOTEPDF-PARK-INCLUDED-DUBAI-FRAME",
        "QUOTEPDF-PARK-EXCLUDED-DUBAI-FRAME",
        "QUOTEPDF-PARK-TERMS-DUBAI-FRAME",
        jpegBytes("DUBAI FRAME"));
    upsertMaster(
        "ITMTESTHOTEL001",
        "Sea View Hotel - Bur Dubai",
        "Hotels",
        "QUOTEPDF-HOTEL-DESC-SEAVIEW",
        "QUOTEPDF-HOTEL-INCLUDED-SEAVIEW",
        "",
        "QUOTEPDF-HOTEL-TERMS-SEAVIEW",
        jpegBytes("SEA VIEW"));

    String payload =
        """
        {"adults":1,"children":0,"markupPercent":0,"items":[
          {"itemId":"dubai-frame","visitDate":"2026-09-21","adults":1,"children":0},
          {"itemId":"dubai-frame","visitDate":"2026-09-22","adults":1,"children":0},
          {"itemId":"sea-view-hotel-bur-dubai","visitDate":"2026-09-22","checkOut":"2026-09-23","adults":1,"children":0,"rooms":1,"roomType":"Standard"},
          {"itemId":"ascort-hotel-bur-dubai","visitDate":"2026-09-22","checkOut":"2026-09-23","adults":1,"children":0,"rooms":1,"roomType":"Deluxe"}
        ]}
        """;

    byte[] pdf =
        mockMvc
            .perform(
                post("/api/parks/quote/pdf")
                    .session(adminSession())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Rathin-Quote-")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    String text = new String(pdf, StandardCharsets.ISO_8859_1);
    org.assertj.core.api.Assertions.assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    org.assertj.core.api.Assertions.assertThat(text)
        .contains("QUOTATION")
        .contains("Quoted itinerary")
        .contains("Grand total")
        .contains("AMOUNT \\(AED\\)")
        .contains("45.00")
        .contains("150.00")
        .doesNotContain("AED 45.00")
        .doesNotContain("AED 150.00")
        .contains("Dubai Frame")
        .contains("PARK DETAILS")
        .contains("HOTEL DETAILS")
        .contains("QUOTEPDF-PARK-DESC-DUBAI-FRAME")
        .contains("QUOTEPDF-PARK-INCLUDED-DUBAI-FRAME")
        .contains("QUOTEPDF-PARK-EXCLUDED-DUBAI-FRAME")
        .contains("QUOTEPDF-PARK-TERMS-DUBAI-FRAME")
        .contains("QUOTEPDF-HOTEL-DESC-SEAVIEW")
        .contains("QUOTEPDF-HOTEL-INCLUDED-SEAVIEW")
        .contains("QUOTEPDF-HOTEL-TERMS-SEAVIEW")
        .contains("Sea View Hotel - Bur Dubai")
        .contains("Ascort Hotel - Bur Dubai")
        .contains("4-star Hotel in Dubai")
        .contains("Room types")
        .contains("Deluxe");
    org.assertj.core.api.Assertions.assertThat(count(text, "PARK DETAILS")).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(count(text, "HOTEL DETAILS")).isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(count(text, "QUOTEPDF-PARK-DESC-DUBAI-FRAME")).isEqualTo(1);
  }

  private MockHttpSession adminSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc
        .perform(apiLogin("Admin@rathin.com", "R@t@2023").session(session))
        .andExpect(status().isOk());
    return session;
  }

  private void upsertMaster(
      String itemId,
      String name,
      String category,
      String description,
      String included,
      String excluded,
      String terms,
      byte[] jpeg)
      throws Exception {
    ItemMaster item = items.findByItemNameIgnoreCase(name).orElseGet(ItemMaster::new);
    Instant now = Instant.now();
    if (item.getItemId() == null) {
      item.setItemId(itemId);
      item.setCreatedDate(now);
      item.setCreatedBy("test");
    }
    item.setItemName(name);
    item.setDatedType(ItemMaster.DATED_TYPE_DA);
    item.setCategory(category);
    item.setBookingInfo(ItemMaster.BOOKING_INFO_DFT);
    item.setDescription(description);
    item.setIncluded(included);
    item.setExcluded(excluded);
    item.setTermsAndCondition(terms);
    item.setUpdatedDate(now);
    item.setUpdatedBy("test");
    String path =
        mediaStore.save(
            item.getItemId(),
            "pdf",
            new MockMultipartFile("imagePdfFile", "item.jpg", "image/jpeg", jpeg));
    item.setImagePdf(path);
    items.save(item);
  }

  private static int count(String text, String needle) {
    int count = 0;
    int from = 0;
    while (true) {
      int at = text.indexOf(needle, from);
      if (at < 0) {
        return count;
      }
      count++;
      from = at + needle.length();
    }
  }

  private static byte[] jpegBytes(String label) throws Exception {
    BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(new Color(0, 87, 184));
    graphics.fillRect(0, 0, 320, 180);
    graphics.setColor(Color.WHITE);
    graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
    graphics.drawString(label, 28, 100);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "jpg", out)) {
      throw new IllegalStateException("Could not write JPEG for " + label);
    }
    return out.toByteArray();
  }
}

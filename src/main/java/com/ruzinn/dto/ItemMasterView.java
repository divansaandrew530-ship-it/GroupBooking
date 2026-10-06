package com.ruzinn.dto;

import com.ruzinn.entity.ItemMaster;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemMasterView(
    String itemId,
    String itemName,
    String country,
    String datedType,
    String category,
    boolean apiTimeslot,
    boolean apiBooking,
    BigDecimal adultRate,
    BigDecimal childRate,
    String bookingInfo,
    List<String> extraFields,
    String description,
    String termsAndCondition,
    String included,
    String excluded,
    String imagePdf,
    String imageTicket,
    Instant createdDate,
    Instant updatedDate) {

  public static ItemMasterView from(ItemMaster item) {
    return new ItemMasterView(
        item.getItemId(),
        item.getItemName(),
        item.getCountry(),
        item.getDatedType(),
        item.getCategory(),
        item.isApiTimeslot(),
        item.isApiBooking(),
        item.getAdultRate(),
        item.getChildRate(),
        item.getBookingInfo(),
        item.extraFields(),
        item.getDescription(),
        item.getTermsAndCondition(),
        item.getIncluded(),
        item.getExcluded(),
        item.getImagePdf(),
        item.getImageTicket(),
        item.getCreatedDate(),
        item.getUpdatedDate());
  }

  public String countryLabel() {
    return com.ruzinn.dto.ItemCountry.find(country).map(com.ruzinn.dto.ItemCountry::name).orElse("United Arab Emirates");
  }

  public String flag() {
    return com.ruzinn.dto.ItemCountry.find(country).map(com.ruzinn.dto.ItemCountry::flag).orElse("ae");
  }

  public String flagUrl() {
    return "/images/flags/" + flag() + ".svg";
  }

  public String apiTimeslotLabel() {
    return apiTimeslot ? "Y" : "N";
  }

  public String apiBookingLabel() {
    return apiBooking ? "Y" : "N";
  }
}

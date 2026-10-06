package com.ruzinn.dto;

import com.ruzinn.entity.ItemMaster;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class ItemMasterForm {

  private String itemId = "";
  private String itemName = "";
  private String country = "UAE";
  private String datedType = ItemMaster.DATED_TYPE_DA;
  private String category = "";
  private boolean apiTimeslot;
  private boolean apiBooking;
  private String adultRate = "";
  private String childRate = "";
  private String bookingInfo = ItemMaster.BOOKING_INFO_DFT;
  private List<String> extraFields = emptyFields();
  private String description = "";
  private String termsAndCondition = "";
  private String included = "";
  private String excluded = "";
  private String imagePdf = "";
  private String imageTicket = "";
  private MultipartFile imagePdfFile;
  private MultipartFile imageTicketFile;

  public static ItemMasterForm blank() {
    return new ItemMasterForm();
  }

  public static ItemMasterForm from(ItemMaster item) {
    ItemMasterForm form = new ItemMasterForm();
    form.itemId = item.getItemId();
    form.itemName = item.getItemName();
    form.country = item.getCountry() == null || item.getCountry().isBlank() ? "UAE" : item.getCountry();
    form.datedType = item.getDatedType();
    form.category = item.getCategory();
    form.apiTimeslot = item.isApiTimeslot();
    form.apiBooking = item.isApiBooking();
    form.adultRate = item.getAdultRate() == null ? "" : item.getAdultRate().toPlainString();
    form.childRate = item.getChildRate() == null ? "" : item.getChildRate().toPlainString();
    form.bookingInfo = item.getBookingInfo();
    form.extraFields = pad(item.extraFields());
    form.description = item.getDescription();
    form.termsAndCondition = item.getTermsAndCondition();
    form.included = item.getIncluded();
    form.excluded = item.getExcluded();
    form.imagePdf = item.getImagePdf();
    form.imageTicket = item.getImageTicket();
    return form;
  }

  public BigDecimal parsedAdultRate() {
    return parseRate(adultRate, "Adult");
  }

  public BigDecimal parsedChildRate() {
    if (childRate == null || childRate.isBlank()) {
      return null;
    }
    return parseRate(childRate, "Child");
  }

  private static BigDecimal parseRate(String value, String label) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(label + " rate must be a number");
    }
  }

  private static List<String> emptyFields() {
    List<String> fields = new ArrayList<>(15);
    for (int i = 0; i < 15; i++) {
      fields.add("");
    }
    return fields;
  }

  private static List<String> pad(List<String> values) {
    List<String> fields = emptyFields();
    if (values == null) {
      return fields;
    }
    for (int i = 0; i < Math.min(15, values.size()); i++) {
      fields.set(i, values.get(i) == null ? "" : values.get(i));
    }
    return fields;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getDatedType() {
    return datedType;
  }

  public void setDatedType(String datedType) {
    this.datedType = datedType;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isApiTimeslot() {
    return apiTimeslot;
  }

  public void setApiTimeslot(boolean apiTimeslot) {
    this.apiTimeslot = apiTimeslot;
  }

  public boolean isApiBooking() {
    return apiBooking;
  }

  public void setApiBooking(boolean apiBooking) {
    this.apiBooking = apiBooking;
  }

  public String getAdultRate() {
    return adultRate;
  }

  public void setAdultRate(String adultRate) {
    this.adultRate = adultRate;
  }

  public String getChildRate() {
    return childRate;
  }

  public void setChildRate(String childRate) {
    this.childRate = childRate;
  }

  public String getBookingInfo() {
    return bookingInfo;
  }

  public void setBookingInfo(String bookingInfo) {
    this.bookingInfo = bookingInfo;
  }

  public List<String> getExtraFields() {
    return extraFields;
  }

  public void setExtraFields(List<String> extraFields) {
    this.extraFields = pad(extraFields);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTermsAndCondition() {
    return termsAndCondition;
  }

  public void setTermsAndCondition(String termsAndCondition) {
    this.termsAndCondition = termsAndCondition;
  }

  public String getIncluded() {
    return included;
  }

  public void setIncluded(String included) {
    this.included = included;
  }

  public String getExcluded() {
    return excluded;
  }

  public void setExcluded(String excluded) {
    this.excluded = excluded;
  }

  public String getImagePdf() {
    return imagePdf;
  }

  public void setImagePdf(String imagePdf) {
    this.imagePdf = imagePdf;
  }

  public String getImageTicket() {
    return imageTicket;
  }

  public void setImageTicket(String imageTicket) {
    this.imageTicket = imageTicket;
  }

  public MultipartFile getImagePdfFile() {
    return imagePdfFile;
  }

  public void setImagePdfFile(MultipartFile imagePdfFile) {
    this.imagePdfFile = imagePdfFile;
  }

  public MultipartFile getImageTicketFile() {
    return imageTicketFile;
  }

  public void setImageTicketFile(MultipartFile imageTicketFile) {
    this.imageTicketFile = imageTicketFile;
  }

  public String pdfPreviewUrl() {
    return previewUrl(imagePdf, "pdf");
  }

  public String ticketPreviewUrl() {
    return previewUrl(imageTicket, "ticket");
  }

  public String imagePdfFileName() {
    return fileName(imagePdf);
  }

  public String imageTicketFileName() {
    return fileName(imageTicket);
  }

  private String previewUrl(String stored, String kind) {
    if (itemId == null || itemId.isBlank() || stored == null || !stored.replace('\\', '/').startsWith("items/")) {
      return "";
    }
    return "/admin/items/" + itemId + "/image/" + kind;
  }

  private static String fileName(String stored) {
    if (stored == null || stored.isBlank()) {
      return "";
    }
    String path = stored.replace('\\', '/');
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }
}

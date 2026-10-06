package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParkQuote {
  private String currency = "AED";
  private int adults;
  private int children;
  private double markupPercent;
  private List<ParkQuoteLine> items = new ArrayList<>();
  private double subtotal;
  private double markupAmount;
  private double total;
  private boolean hasOnRequest;
  private String quoteNumber;
  private String issuedOn;
  private String preparedBy;
  private String companyName;
  private String companyId;
  private String brandAddress = "";
  private String brandEmail = "";
  private String brandPhone = "";
  private String brandWebsite = "";
  private String brandPlace = "";
  private boolean hasLogo;
  private String logoUrl = "";
  private String country;
  private double aedSubtotal;
  private double aedMarkupAmount;
  private double aedTotal;
  private double roeRate = 1;
  private double convertedValue;
  private boolean directCustomer;
  private String customerName = "";
  private String customerEmail = "";
  private String customerPhone = "";
  private String packageInclusions = "";
  private String packageExclusions = "";
  private String guestInformation = "";
  private List<ParkDayPlan> dayPlans = new ArrayList<>();
  private LocalDate expireDate;

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAdults() {
    return adults;
  }

  public void setAdults(int adults) {
    this.adults = adults;
  }

  public int getChildren() {
    return children;
  }

  public void setChildren(int children) {
    this.children = children;
  }

  public double getMarkupPercent() {
    return markupPercent;
  }

  public void setMarkupPercent(double markupPercent) {
    this.markupPercent = markupPercent;
  }

  public List<ParkQuoteLine> getItems() {
    return items;
  }

  public void setItems(List<ParkQuoteLine> items) {
    this.items = items;
  }

  public double getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(double subtotal) {
    this.subtotal = subtotal;
  }

  public double getMarkupAmount() {
    return markupAmount;
  }

  public void setMarkupAmount(double markupAmount) {
    this.markupAmount = markupAmount;
  }

  public double getTotal() {
    return total;
  }

  public void setTotal(double total) {
    this.total = total;
  }

  public boolean isHasOnRequest() {
    return hasOnRequest;
  }

  public void setHasOnRequest(boolean hasOnRequest) {
    this.hasOnRequest = hasOnRequest;
  }

  public String getQuoteNumber() {
    return quoteNumber;
  }

  public void setQuoteNumber(String quoteNumber) {
    this.quoteNumber = quoteNumber;
  }

  public String getIssuedOn() {
    return issuedOn;
  }

  public void setIssuedOn(String issuedOn) {
    this.issuedOn = issuedOn;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public void setPreparedBy(String preparedBy) {
    this.preparedBy = preparedBy;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getBrandAddress() {
    return brandAddress;
  }

  public void setBrandAddress(String brandAddress) {
    this.brandAddress = brandAddress == null ? "" : brandAddress;
  }

  public String getBrandEmail() {
    return brandEmail;
  }

  public void setBrandEmail(String brandEmail) {
    this.brandEmail = brandEmail == null ? "" : brandEmail;
  }

  public String getBrandPhone() {
    return brandPhone;
  }

  public void setBrandPhone(String brandPhone) {
    this.brandPhone = brandPhone == null ? "" : brandPhone;
  }

  public String getBrandWebsite() {
    return brandWebsite;
  }

  public void setBrandWebsite(String brandWebsite) {
    this.brandWebsite = brandWebsite == null ? "" : brandWebsite;
  }

  public String getBrandPlace() {
    return brandPlace;
  }

  public void setBrandPlace(String brandPlace) {
    this.brandPlace = brandPlace == null ? "" : brandPlace;
  }

  public boolean isHasLogo() {
    return hasLogo;
  }

  public void setHasLogo(boolean hasLogo) {
    this.hasLogo = hasLogo;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl == null ? "" : logoUrl;
  }

  public String getBrandName() {
    return companyName == null || companyName.isBlank() ? "Rathin" : companyName.trim();
  }

  public String getBrandFooter() {
    java.util.List<String> parts = new java.util.ArrayList<>();
    addBrandPart(parts, getBrandName());
    addBrandPart(parts, brandAddress);
    addBrandPart(parts, brandPlace);
    addBrandPart(parts, brandEmail);
    addBrandPart(parts, brandPhone);
    addBrandPart(parts, brandWebsite);
    return String.join("  ·  ", parts);
  }

  public String getBrandContact() {
    java.util.List<String> parts = new java.util.ArrayList<>();
    addBrandPart(parts, brandAddress);
    addBrandPart(parts, brandPlace);
    addBrandPart(parts, brandEmail);
    addBrandPart(parts, brandPhone);
    addBrandPart(parts, brandWebsite);
    return String.join("  ·  ", parts);
  }

  private static void addBrandPart(java.util.List<String> parts, String value) {
    if (value != null && !value.isBlank()) {
      parts.add(value.trim());
    }
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public double getAedSubtotal() {
    return aedSubtotal;
  }

  public void setAedSubtotal(double aedSubtotal) {
    this.aedSubtotal = aedSubtotal;
  }

  public double getAedMarkupAmount() {
    return aedMarkupAmount;
  }

  public void setAedMarkupAmount(double aedMarkupAmount) {
    this.aedMarkupAmount = aedMarkupAmount;
  }

  public double getAedTotal() {
    return aedTotal;
  }

  public void setAedTotal(double aedTotal) {
    this.aedTotal = aedTotal;
  }

  public double getRoeRate() {
    return roeRate;
  }

  public void setRoeRate(double roeRate) {
    this.roeRate = roeRate;
  }

  public double getConvertedValue() {
    return convertedValue;
  }

  public void setConvertedValue(double convertedValue) {
    this.convertedValue = convertedValue;
  }

  public double creditTotal() {
    return aedTotal;
  }

  public String getRoeLabel() {
    return BookingPresentation.formatRoe(currency, BigDecimal.valueOf(roeRate));
  }

  public boolean isDirectCustomer() {
    return directCustomer;
  }

  public void setDirectCustomer(boolean directCustomer) {
    this.directCustomer = directCustomer;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName == null ? "" : customerName;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail == null ? "" : customerEmail;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone == null ? "" : customerPhone;
  }

  public String getPackageInclusions() {
    return packageInclusions;
  }

  public void setPackageInclusions(String packageInclusions) {
    this.packageInclusions = packageInclusions == null ? "" : packageInclusions;
  }

  public String getPackageExclusions() {
    return packageExclusions;
  }

  public void setPackageExclusions(String packageExclusions) {
    this.packageExclusions = packageExclusions == null ? "" : packageExclusions;
  }

  public String getGuestInformation() {
    return guestInformation;
  }

  public void setGuestInformation(String guestInformation) {
    this.guestInformation = guestInformation == null ? "" : guestInformation;
  }

  public List<ParkDayPlan> getDayPlans() {
    return dayPlans;
  }

  public void setDayPlans(List<ParkDayPlan> dayPlans) {
    this.dayPlans = dayPlans == null ? new ArrayList<>() : dayPlans;
  }

  public LocalDate getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(LocalDate expireDate) {
    this.expireDate = expireDate;
  }

  public LocalDate resolvedExpireDate() {
    return BookingPresentation.quoteExpireDate(expireDate, null);
  }

  public String getExpireDateLabel() {
    return BookingPresentation.formatTravel(resolvedExpireDate());
  }

  public String getExpireDateIso() {
    return resolvedExpireDate().toString();
  }

  public String getValidityCopy() {
    return BookingPresentation.quoteValidityCopy(resolvedExpireDate());
  }

  public String getPackageTitle() {
    String code = country == null || country.isBlank() ? ItemCountry.UAE : country.trim();
    return switch (code.toUpperCase()) {
      case "IND" -> "India Holiday Package";
      case "SIN" -> "Singapore Holiday Package";
      default -> "UAE Holiday Package";
    };
  }

  public String getDurationLabel() {
    int[] nightsAndDays = nightsAndDays();
    if (nightsAndDays == null) {
      return "";
    }
    return nightsAndDays[0] + "N / " + nightsAndDays[1] + "D";
  }

  public String getDurationPhrase() {
    int[] nightsAndDays = nightsAndDays();
    if (nightsAndDays == null) {
      return "";
    }
    return nightsAndDays[0]
        + (nightsAndDays[0] == 1 ? " night / " : " nights / ")
        + nightsAndDays[1]
        + (nightsAndDays[1] == 1 ? " day" : " days");
  }

  public String getDateRangeLabel() {
    return dateRange(" – ");
  }

  public String getPrintDateRange() {
    return dateRange(" - ");
  }

  public String getGuestLabel() {
    String label = adults + (adults == 1 ? " Adult" : " Adults");
    if (children > 0) {
      label += " · " + children + (children == 1 ? " Child" : " Children");
    }
    return label;
  }

  public String getStaySummary() {
    ParkQuoteLine stay = stayLine();
    if (stay == null) {
      return "";
    }
    StringBuilder summary = new StringBuilder(stay.getName() == null ? "Hotel" : stay.getName());
    if (stay.getRoomType() != null && !stay.getRoomType().isBlank()) {
      summary.append(" · ").append(stay.getRoomType().trim());
    }
    Integer nights = stay.getNights();
    if (nights != null) {
      summary.append(" · ").append(nights).append(nights == 1 ? " night" : " nights");
    }
    if (stay.getRooms() > 0) {
      summary.append(" · ").append(stay.getRooms()).append(stay.getRooms() == 1 ? " room" : " rooms");
    }
    return summary.toString();
  }

  public String getHotelName() {
    ParkQuoteLine stay = stayLine();
    if (stay == null || stay.getName() == null || stay.getName().isBlank()) {
      return "";
    }
    return stay.getName().trim();
  }

  public String getStayDetailLine() {
    ParkQuoteLine stay = stayLine();
    if (stay == null) {
      return "";
    }
    java.util.List<String> parts = new ArrayList<>();
    if (stay.getRoomType() != null && !stay.getRoomType().isBlank()) {
      String room = stay.getRoomType().trim();
      if (!room.toLowerCase().contains("room")) {
        room = room + " room";
      }
      parts.add(room);
    }
    Integer nights = stay.getNights();
    if (nights != null && nights > 0) {
      parts.add(nights + (nights == 1 ? " night" : " nights"));
    }
    if (stay.getRooms() > 0) {
      parts.add(stay.getRooms() + (stay.getRooms() == 1 ? " room" : " rooms"));
    }
    return String.join(" | ", parts);
  }

  public String getPackageAmountLabel() {
    java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    String code = currency == null || currency.isBlank() ? "AED" : currency.trim();
    return format.format(total) + " " + code;
  }

  public Integer stayNights() {
    ParkQuoteLine stay = stayLine();
    return stay == null ? null : stay.getNights();
  }

  private int[] nightsAndDays() {
    Integer nights = stayNights();
    if (nights != null && nights > 0) {
      return new int[] {nights, nights + 1};
    }
    int days = dayPlans == null ? 0 : dayPlans.size();
    if (days <= 0) {
      return null;
    }
    return new int[] {Math.max(0, days - 1), days};
  }

  private String dateRange(String separator) {
    ParkQuoteLine stay = stayLine();
    String start = stay != null ? stay.getDisplayVisitDate() : firstDayLabel();
    String end = stay != null ? stay.getDisplayCheckOut() : lastDayLabel();
    if (start == null || start.isBlank() || "Date not set".equals(start)) {
      return "";
    }
    if (end == null || end.isBlank() || "Date not set".equals(end)) {
      return start;
    }
    return start + separator + end;
  }

  private ParkQuoteLine stayLine() {
    if (items == null) {
      return null;
    }
    return items.stream().filter(ParkQuoteLine::isStay).findFirst().orElse(null);
  }

  private String firstDayLabel() {
    if (dayPlans == null || dayPlans.isEmpty() || dayPlans.get(0).getDate() == null) {
      return "";
    }
    return formatPlanDate(dayPlans.get(0).getDate());
  }

  private String lastDayLabel() {
    if (dayPlans == null || dayPlans.isEmpty()) {
      return "";
    }
    ParkDayPlan last = dayPlans.get(dayPlans.size() - 1);
    return last.getDate() == null ? "" : formatPlanDate(last.getDate());
  }

  private static String formatPlanDate(String iso) {
    try {
      return BookingPresentation.formatTravel(LocalDate.parse(iso.trim()));
    } catch (Exception ex) {
      return iso;
    }
  }
}

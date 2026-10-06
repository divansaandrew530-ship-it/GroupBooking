package com.ruzinn.dto;

import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** One booked item. {@code passengers} are the guests entered at checkout (name, passport, DOB, ...). */
public record BookingDetailView(
        Long id,
        String bookingRefNum,
        String serviceName,
        String serviceId,
        String itemName,
        LocalDate showDate,
        LocalDate checkinDate,
        LocalDate checkoutDate,
        String propertyId,
        int adultCount,
        int childCount,
        int infantCount,
        int toddlerCount,
        BigDecimal totalRate,
        BigDecimal roeRate,
        BigDecimal convertedRate,
        String currency,
        Instant bookedOn,
        String bookedBy,
        String userId,
        String companyName,
        String companyId,
        String inventoryType,
        String supplierName,
        String supplierId,
        String status,
        String followUp,
        String voucherFileName,
        String ticketFileName,
        BigDecimal actualTotalRate,
        BigDecimal actualConvertedRate,
        boolean hideQuotedRate,
        boolean showQuotedRate,
        List<Map<String, String>> passengers) {

  public String statusLabel() {
    return BookingPresentation.label(status);
  }

  public String statusTone() {
    return BookingPresentation.tone(status);
  }

  public boolean hasVoucher() {
    return voucherFileName != null && !voucherFileName.isBlank();
  }

  public boolean hasTicket() {
    return ticketFileName != null && !ticketFileName.isBlank();
  }

  public boolean hasPreferredDocument() {
    return hasDocument();
  }

  public String documentKind() {
    return BookingPresentation.documentKind(inventoryType);
  }

  public String documentLabel() {
    return BookingPresentation.documentLabel(documentKind());
  }

  public boolean hasDocument() {
    return BookingPresentation.storesAsTicket(documentKind()) ? hasTicket() : hasVoucher();
  }

  public String documentFileName() {
    if (BookingPresentation.storesAsTicket(documentKind())) {
      return ticketFileName;
    }
    return voucherFileName;
  }

  public String documentEmptyLabel() {
    return "No " + documentLabel().toLowerCase() + " uploaded yet.";
  }

  public String documentUploadLabel() {
    return "Upload " + documentLabel().toLowerCase();
  }

  public String documentDownloadLabel() {
    return "Download " + documentLabel().toLowerCase();
  }

  public String followUpLabel() {
    return followUp == null ? "" : followUp;
  }

  public String serviceLine() {
    String service = serviceName == null ? "" : serviceName.trim();
    String inventory = inventoryType == null ? "" : inventoryType.trim();
    if (service.isEmpty()) {
      return inventory;
    }
    if (inventory.isEmpty()) {
      return service;
    }
    return service + " · " + inventory;
  }

  public String datesLabel() {
    if (checkinDate != null) {
      String stay = "Stay " + BookingPresentation.formatTravel(checkinDate);
      if (checkoutDate != null) {
        stay += " → " + BookingPresentation.formatTravel(checkoutDate);
      }
      return stay;
    }
    if (showDate != null && !isVisa()) {
      return "Visit " + BookingPresentation.formatTravel(showDate);
    }
    return "";
  }

  public String guestsLabel() {
    return adultCount + " adult(s) · " + childCount + " child(ren)";
  }

  public String amountLabel() {
    return BookingPresentation.formatAmount(displayAmount(), currency);
  }

  public String quotedAmountLabel() {
    return BookingPresentation.formatAmount(
        convertedRate != null ? convertedRate : totalRate, currency);
  }

  public String actualAmountLabel() {
    return BookingPresentation.formatAmount(
        actualConvertedRate != null
            ? actualConvertedRate
            : (actualTotalRate != null ? actualTotalRate : (convertedRate != null ? convertedRate : totalRate)),
        currency);
  }

  public BigDecimal displayAmount() {
    if (hideQuotedRate) {
      if (actualConvertedRate != null) {
        return actualConvertedRate;
      }
      if (actualTotalRate != null) {
        return actualTotalRate;
      }
    }
    return convertedRate != null ? convertedRate : totalRate;
  }

  public String rateKindLabel() {
    if (hideQuotedRate) {
      return "Actual rate";
    }
    if (showQuotedRate) {
      return "Quoted Rate";
    }
    return "";
  }

  public BookingDetailView forViewer(boolean parentViewer, boolean createdByChild) {
    boolean hide = parentViewer && createdByChild;
    boolean showQuoted = !parentViewer && createdByChild;
    BigDecimal shownTotal = hide && actualTotalRate != null ? actualTotalRate : totalRate;
    BigDecimal shownConverted = hide && actualConvertedRate != null ? actualConvertedRate : convertedRate;
    return new BookingDetailView(
        id,
        bookingRefNum,
        serviceName,
        serviceId,
        itemName,
        showDate,
        checkinDate,
        checkoutDate,
        propertyId,
        adultCount,
        childCount,
        infantCount,
        toddlerCount,
        shownTotal,
        roeRate,
        shownConverted,
        currency,
        bookedOn,
        bookedBy,
        userId,
        companyName,
        companyId,
        inventoryType,
        supplierName,
        supplierId,
        status,
        followUp,
        voucherFileName,
        ticketFileName,
        actualTotalRate,
        actualConvertedRate,
        hide,
        showQuoted,
        passengers);
  }

  public String roeRateLabel() {
    return BookingPresentation.formatRoe(currency, roeRate);
  }

  public String convertedAmountLabel() {
    return amountLabel();
  }

  public int passengerCount() {
    return adultCount + childCount + infantCount + toddlerCount;
  }

  public String travelDateLabel() {
    LocalDate date = showDate != null ? showDate : checkinDate;
    return date == null ? "" : BookingPresentation.formatTravel(date);
  }

  public String itemSummaryLabel() {
    int guests = passengerCount();
    String guestWord = guests == 1 ? "passenger" : "passengers";
    return "1 item · " + guests + " " + guestWord;
  }

  public String listAction() {
    return switch (statusTone()) {
      case "confirmed" -> "download";
      case "pending", "requested", "rejected" -> "edit";
      case "completed" -> "upload";
      default -> "none";
    };
  }

  private boolean isVisa() {
    return inventoryType != null && "VISA".equalsIgnoreCase(inventoryType.trim());
  }
}

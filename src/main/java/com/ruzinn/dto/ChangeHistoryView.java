package com.ruzinn.dto;

import com.ruzinn.entity.ChangeHistory;
import com.ruzinn.util.BookingPresentation;

public record ChangeHistoryView(
    Long id,
    String entityType,
    String entityId,
    String bookingRefNum,
    String action,
    String fieldName,
    String oldValue,
    String newValue,
    String actorUserName,
    String actorEmail,
    String actorRole,
    java.time.Instant changedAt) {

  public static ChangeHistoryView from(ChangeHistory row) {
    return new ChangeHistoryView(
        row.getId(),
        row.getEntityType(),
        row.getEntityId(),
        row.getBookingRefNum(),
        row.getAction(),
        row.getFieldName(),
        row.getOldValue(),
        row.getNewValue(),
        row.getActorUserName(),
        row.getActorEmail(),
        row.getActorRole(),
        row.getChangedAt());
  }

  public String actionLabel() {
    return switch (action == null ? "" : action) {
      case "CREATE" -> "Created";
      case "STATUS_CHANGE" -> "Status change";
      case "FOLLOW_UP" -> "Follow-up";
      case "DOCUMENT_UPLOAD" -> "Document upload";
      case "COMPANY_REVIEW" -> "Company review";
      case "CREDIT_CHARGE" -> "Credit charge";
      case "USER_CREATE" -> "User added";
      case "PASSWORD_RESET" -> "Password reset";
      case "CMS_SAVE" -> "CMS_TRANSLATION".equals(entityType) ? "CMS saved" : "Login home saved";
      case "SLIDE_ADD" -> "Slide added";
      case "SLIDE_DELETE" -> "Slide removed";
      case "ITEM_SAVE" -> "Item saved";
      case "ITEM_DELETE" -> "Item deleted";
      case "ITEM_IMPORT" -> "Item import";
      case "QUOTE_EDIT" -> "Quote edit";
      default -> action == null || action.isBlank() ? "Update" : action;
    };
  }

  public String fieldLabel() {
    String name = fieldName == null ? "" : fieldName;
    int itemSep = name.lastIndexOf(" · ");
    if (itemSep > 0) {
      String item = name.substring(0, itemSep).trim();
      String field = bareFieldLabel(name.substring(itemSep + 3).trim());
      if (item.isBlank()) {
        return field;
      }
      return field.isBlank() ? item : item + " · " + field;
    }
    return bareFieldLabel(name);
  }

  private static String bareFieldLabel(String name) {
    if (name.matches("field([1-9]|1[0-5])")) {
      return "Fields" + name.substring(5);
    }
    return switch (name) {
      case "booking_status" -> "Booking status";
      case "item_status" -> "Item status";
      case "quote_status" -> "Quote status";
      case "booking_ref_num" -> "Booking";
      case "quote_value" -> "Quote value";
      case "itinerary" -> "Itinerary";
      case "sent_to" -> "Sent to";
      case "follow_up" -> "Follow-up";
      case "ticket_file_name" -> "Ticket";
      case "voucher_file_name" -> "Voucher";
      case "item_name" -> "Item";
      case "status" -> "Status";
      case "credit_limit" -> "Credit limit";
      case "credit_used" -> "Credit used";
      case "review_note" -> "Review note";
      case "email" -> "Email";
      case "password" -> "Password";
      case "role" -> "Role";
      case "product_name" -> "Product name";
      case "sign_in_title" -> "Sign in title";
      case "slide_title" -> "Slide title";
      case "dated_type" -> "Dated type";
      case "category" -> "Category";
      case "country" -> "Country";
      case "adult_rate" -> "Adult rate";
      case "child_rate" -> "Child rate";
      case "api_timeslot" -> "API timeslot";
      case "api_booking" -> "API booking";
      case "booking_info" -> "Booking info";
      case "description" -> "Description";
      case "terms_and_condition" -> "Terms and condition";
      case "included" -> "Included";
      case "excluded" -> "Excluded";
      case "line_total" -> "Amount";
      case "item_content" -> "Quote item content";
      case "image_pdf" -> "Image in PDF";
      case "image_ticket" -> "Image in ticket";
      default -> name.isBlank() ? "" : name.replace('_', ' ');
    };
  }

  public String actorLabel() {
    if (actorUserName != null && !actorUserName.isBlank()) {
      return actorUserName;
    }
    if (actorEmail != null && !actorEmail.isBlank()) {
      return actorEmail;
    }
    return "System";
  }

  public String changedAtLabel() {
    return BookingPresentation.formatCreated(changedAt);
  }

  public boolean hasValueChange() {
    return (oldValue != null && !oldValue.isBlank()) || (newValue != null && !newValue.isBlank());
  }

  public boolean hasOldValue() {
    return oldValue != null && !oldValue.isBlank();
  }

  public boolean hasNewValue() {
    return newValue != null && !newValue.isBlank();
  }

  public String oldValueLabel() {
    return displayValue(oldValue);
  }

  public String newValueLabel() {
    return displayValue(newValue);
  }

  public String valueChangeLabel() {
    if (!hasValueChange()) {
      return "";
    }
    String from = displayValue(oldValue);
    String to = displayValue(newValue);
    if ("—".equals(from)) {
      return to;
    }
    return from + " → " + to;
  }

  public String summary() {
    String action = actionLabel();
    String field = fieldLabel();
    if (field.isBlank() || field.equalsIgnoreCase(action)) {
      return action;
    }
    return action + " · " + field;
  }

  private static String displayValue(String value) {
    if (value == null || value.isBlank()) {
      return "—";
    }
    return value;
  }
}

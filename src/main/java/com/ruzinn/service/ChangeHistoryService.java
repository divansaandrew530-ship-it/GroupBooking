package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ChangeHistoryView;
import java.util.List;

public interface ChangeHistoryService {

  String BOOKING_MAIN = "BOOKING_MAIN";
  String BOOKING_DETAIL = "BOOKING_DETAIL";
  String QUOTE_MAIN = "QUOTE_MAIN";
  String QUOTE_DETAIL = "QUOTE_DETAIL";
  String COMPANY = "COMPANY";
  String APP_USER = "APP_USER";
  String LOGIN_HOME = "LOGIN_HOME";
  String ITEM_MASTER = "ITEM_MASTER";
  String CMS_TRANSLATION = "CMS_TRANSLATION";

  String CREATE = "CREATE";
  String STATUS_CHANGE = "STATUS_CHANGE";
  String FOLLOW_UP = "FOLLOW_UP";
  String DOCUMENT_UPLOAD = "DOCUMENT_UPLOAD";
  String COMPANY_REVIEW = "COMPANY_REVIEW";
  String CREDIT_CHARGE = "CREDIT_CHARGE";
  String USER_CREATE = "USER_CREATE";
  String PASSWORD_RESET = "PASSWORD_RESET";
  String CMS_SAVE = "CMS_SAVE";
  String SLIDE_ADD = "SLIDE_ADD";
  String SLIDE_DELETE = "SLIDE_DELETE";
  String ITEM_SAVE = "ITEM_SAVE";
  String ITEM_DELETE = "ITEM_DELETE";
  String ITEM_IMPORT = "ITEM_IMPORT";
  String QUOTE_EDIT = "QUOTE_EDIT";

  void record(
      String entityType,
      String entityId,
      String bookingRefNum,
      String action,
      String fieldName,
      Object oldValue,
      Object newValue,
      String companyId,
      String companyName);

  void record(
      String entityType,
      String entityId,
      String bookingRefNum,
      String action,
      String fieldName,
      Object oldValue,
      Object newValue,
      String companyId,
      String companyName,
      AuthSession actor);

  List<ChangeHistoryView> listForBooking(String bookingRefNum);

  List<ChangeHistoryView> listForEntity(String entityType, String entityId);

  List<ChangeHistoryView> listForEntityType(String entityType);
}

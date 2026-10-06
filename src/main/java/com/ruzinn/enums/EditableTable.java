package com.ruzinn.enums;

public enum EditableTable {
  BOOKING_MAIN("booking_main", "booking_ref_num", "BOOKING_MAIN"),
  BOOKING_DETAIL("booking_details", "id", "BOOKING_DETAIL"),
  QUOTE_MAIN("quote_main", "quote_ref_num", "QUOTE_MAIN"),
  QUOTE_DETAIL("quote_details", "id", "QUOTE_DETAIL"),
  COMPANY("company", "company_id", "COMPANY"),
  APP_USER("app_user", "user_id", "APP_USER"),
  COMPANY_REGISTRATION("company_registration", "id", "COMPANY_REGISTRATION"),
  LOGIN_HOME_PAGE("login_home_page", "page_id", "LOGIN_HOME"),
  LOGIN_HOME_SLIDE("login_home_slide", "id", "LOGIN_HOME_SLIDE"),
  LOGIN_HOME_FEATURE("login_home_feature", "id", "LOGIN_HOME_FEATURE"),
  ITEM_MASTER("item_master", "item_id", "ITEM_MASTER"),
  CMS_TRANSLATION("cms_translation", "id", "CMS_TRANSLATION");

  private final String sourceTable;
  private final String primaryKeyColumn;
  private final String entityType;

  EditableTable(String sourceTable, String primaryKeyColumn, String entityType) {
    this.sourceTable = sourceTable;
    this.primaryKeyColumn = primaryKeyColumn;
    this.entityType = entityType;
  }

  public String sourceTable() {
    return sourceTable;
  }

  public String historyTable() {
    return sourceTable + "_Hstry";
  }

  public String primaryKeyColumn() {
    return primaryKeyColumn;
  }

  public String entityType() {
    return entityType;
  }
}

package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "cms_translation",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_cms_translation_key",
            columnNames = {"bundle", "entity_id", "field_name", "locale"}),
    indexes = @Index(name = "idx_cms_translation_locale", columnList = "bundle, locale"))
public class CmsTranslation {

  public static final String BUNDLE_UI = "UI";
  public static final String BUNDLE_LOGIN_HOME = "LOGIN_HOME";
  public static final String BUNDLE_ITEM_MASTER = "ITEM_MASTER";
  public static final String PAGE_ID = "page";
  public static final String UI_ENTITY = "-";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "bundle", nullable = false, length = 40)
  private String bundle = BUNDLE_UI;

  @Column(name = "entity_id", nullable = false, length = 80)
  private String entityId = UI_ENTITY;

  @Column(name = "field_name", nullable = false, length = 80)
  private String fieldName = "";

  @Column(name = "locale", nullable = false, length = 8)
  private String locale = "en";

  @Column(name = "text_value", nullable = false, columnDefinition = "text")
  private String textValue = "";

  @Column(name = "updated_date", nullable = false)
  private Instant updatedDate = Instant.now();

  @Column(name = "updated_by", length = 128)
  private String updatedBy = "";

  public Long getId() {
    return id;
  }

  public String getBundle() {
    return bundle;
  }

  public void setBundle(String bundle) {
    this.bundle = bundle;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId == null || entityId.isBlank() ? UI_ENTITY : entityId;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue == null ? "" : textValue;
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy == null ? "" : updatedBy;
  }
}

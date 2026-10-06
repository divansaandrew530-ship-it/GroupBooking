package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "change_history",
    indexes = {
      @Index(name = "idx_change_history_booking", columnList = "booking_ref_num, changed_at"),
      @Index(name = "idx_change_history_entity", columnList = "entity_type, entity_id"),
      @Index(name = "idx_change_history_changed_at", columnList = "changed_at")
    })
public class ChangeHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "entity_type", nullable = false, length = 40)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 80)
  private String entityId;

  @Column(name = "booking_ref_num", length = 32)
  private String bookingRefNum;

  @Column(name = "action", nullable = false, length = 40)
  private String action;

  @Column(name = "field_name", length = 80)
  private String fieldName;

  @Column(name = "old_value", length = 2000)
  private String oldValue;

  @Column(name = "new_value", length = 2000)
  private String newValue;

  @Column(name = "actor_user_id", length = 32)
  private String actorUserId;

  @Column(name = "actor_user_name", length = 255)
  private String actorUserName;

  @Column(name = "actor_email", length = 160)
  private String actorEmail;

  @Column(name = "actor_role", length = 40)
  private String actorRole;

  @Column(name = "company_id", length = 64)
  private String companyId;

  @Column(name = "company_name", length = 255)
  private String companyName;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  public Long getId() {
    return id;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getBookingRefNum() {
    return bookingRefNum;
  }

  public void setBookingRefNum(String bookingRefNum) {
    this.bookingRefNum = bookingRefNum;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public String getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(String actorUserId) {
    this.actorUserId = actorUserId;
  }

  public String getActorUserName() {
    return actorUserName;
  }

  public void setActorUserName(String actorUserName) {
    this.actorUserName = actorUserName;
  }

  public String getActorEmail() {
    return actorEmail;
  }

  public void setActorEmail(String actorEmail) {
    this.actorEmail = actorEmail;
  }

  public String getActorRole() {
    return actorRole;
  }

  public void setActorRole(String actorRole) {
    this.actorRole = actorRole;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(Instant changedAt) {
    this.changedAt = changedAt;
  }
}

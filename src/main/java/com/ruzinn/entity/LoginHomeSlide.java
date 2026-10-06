package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_home_slide")
public class LoginHomeSlide {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "page_id", nullable = false)
  private LoginHomePage page;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "image_path", length = 512)
  private String imagePath = "";

  @Column(name = "image_content_type", length = 120)
  private String imageContentType = "image/jpeg";

  @Column(name = "eyebrow", nullable = false, length = 80)
  private String eyebrow = "";

  @Column(name = "title", nullable = false, length = 160)
  private String title = "";

  @Column(name = "accent_phrase", length = 80)
  private String accentPhrase = "";

  @Column(name = "script_text", length = 80)
  private String scriptText = "";

  @Column(name = "badge_kicker", length = 40)
  private String badgeKicker = "";

  @Column(name = "badge_value", length = 40)
  private String badgeValue = "";

  @Column(name = "badge_sub", length = 40)
  private String badgeSub = "";

  @Column(name = "body", length = 400)
  private String body = "";

  @Column(name = "cta_label", length = 80)
  private String ctaLabel = "Book Group Deals";

  @Column(name = "cta_href", length = 200)
  private String ctaHref = "/register";

  public Long getId() {
    return id;
  }

  public LoginHomePage getPage() {
    return page;
  }

  public void setPage(LoginHomePage page) {
    this.page = page;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public String getImageContentType() {
    return imageContentType;
  }

  public void setImageContentType(String imageContentType) {
    this.imageContentType = imageContentType;
  }

  public String getEyebrow() {
    return eyebrow;
  }

  public void setEyebrow(String eyebrow) {
    this.eyebrow = eyebrow;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAccentPhrase() {
    return accentPhrase;
  }

  public void setAccentPhrase(String accentPhrase) {
    this.accentPhrase = accentPhrase;
  }

  public String getScriptText() {
    return scriptText;
  }

  public void setScriptText(String scriptText) {
    this.scriptText = scriptText;
  }

  public String getBadgeKicker() {
    return badgeKicker;
  }

  public void setBadgeKicker(String badgeKicker) {
    this.badgeKicker = badgeKicker;
  }

  public String getBadgeValue() {
    return badgeValue;
  }

  public void setBadgeValue(String badgeValue) {
    this.badgeValue = badgeValue;
  }

  public String getBadgeSub() {
    return badgeSub;
  }

  public void setBadgeSub(String badgeSub) {
    this.badgeSub = badgeSub;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getCtaLabel() {
    return ctaLabel;
  }

  public void setCtaLabel(String ctaLabel) {
    this.ctaLabel = ctaLabel;
  }

  public String getCtaHref() {
    return ctaHref;
  }

  public void setCtaHref(String ctaHref) {
    this.ctaHref = ctaHref;
  }
}

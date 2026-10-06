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
@Table(name = "login_home_feature")
public class LoginHomeFeature {

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

  @Column(name = "icon_key", nullable = false, length = 40)
  private String iconKey = "flights";

  @Column(name = "title", nullable = false, length = 80)
  private String title = "";

  @Column(name = "subtitle", nullable = false, length = 120)
  private String subtitle = "";

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

  public String getIconKey() {
    return iconKey;
  }

  public void setIconKey(String iconKey) {
    this.iconKey = iconKey;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }
}

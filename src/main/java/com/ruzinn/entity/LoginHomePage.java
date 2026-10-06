package com.ruzinn.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "login_home_page")
public class LoginHomePage {

  public static final long SINGLETON_ID = 1L;

  @Id
  @Column(name = "page_id")
  private Long id = SINGLETON_ID;

  @Column(name = "product_name", nullable = false, length = 80)
  private String productName = "GROUPBOOKINGLA";

  @Column(name = "product_pill", nullable = false, length = 80)
  private String productPill = "B2B company group booking";

  @Column(name = "panel_tagline", nullable = false, length = 200)
  private String panelTagline = "Travel Together  •  Better Business  •  Greater Possibilities";

  @Column(name = "sign_in_title", nullable = false, length = 80)
  private String signInTitle = "Sign In";

  @Column(name = "sign_in_subtitle", nullable = false, length = 200)
  private String signInSubtitle = "Please enter your credentials to sign in!";

  @Column(name = "register_label", nullable = false, length = 40)
  private String registerLabel = "Register";

  @Column(name = "footer_copy", nullable = false, length = 200)
  private String footerCopy = "© 2026 Rathin. All rights reserved.";

  @Column(name = "terms_href", nullable = false, length = 200)
  private String termsHref = "#terms";

  @Column(name = "privacy_href", nullable = false, length = 200)
  private String privacyHref = "#privacy";

  @Column(name = "support_href", nullable = false, length = 200)
  private String supportHref = "#support";

  @Column(name = "updated_date", nullable = false)
  private Instant updatedDate = Instant.now();

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  private List<LoginHomeSlide> slides = new ArrayList<>();

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  private List<LoginHomeFeature> features = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductPill() {
    return productPill;
  }

  public void setProductPill(String productPill) {
    this.productPill = productPill;
  }

  public String getPanelTagline() {
    return panelTagline;
  }

  public void setPanelTagline(String panelTagline) {
    this.panelTagline = panelTagline;
  }

  public String getSignInTitle() {
    return signInTitle;
  }

  public void setSignInTitle(String signInTitle) {
    this.signInTitle = signInTitle;
  }

  public String getSignInSubtitle() {
    return signInSubtitle;
  }

  public void setSignInSubtitle(String signInSubtitle) {
    this.signInSubtitle = signInSubtitle;
  }

  public String getRegisterLabel() {
    return registerLabel;
  }

  public void setRegisterLabel(String registerLabel) {
    this.registerLabel = registerLabel;
  }

  public String getFooterCopy() {
    return footerCopy;
  }

  public void setFooterCopy(String footerCopy) {
    this.footerCopy = footerCopy;
  }

  public String getTermsHref() {
    return termsHref;
  }

  public void setTermsHref(String termsHref) {
    this.termsHref = termsHref;
  }

  public String getPrivacyHref() {
    return privacyHref;
  }

  public void setPrivacyHref(String privacyHref) {
    this.privacyHref = privacyHref;
  }

  public String getSupportHref() {
    return supportHref;
  }

  public void setSupportHref(String supportHref) {
    this.supportHref = supportHref;
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }

  public List<LoginHomeSlide> getSlides() {
    return slides;
  }

  public List<LoginHomeFeature> getFeatures() {
    return features;
  }

  public void addSlide(LoginHomeSlide slide) {
    slide.setPage(this);
    slides.add(slide);
  }

  public void addFeature(LoginHomeFeature feature) {
    feature.setPage(this);
    features.add(feature);
  }
}

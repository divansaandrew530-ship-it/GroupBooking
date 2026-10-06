package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class LoginHomeForm {

  private String productName = "";
  private String productPill = "";
  private String panelTagline = "";
  private String signInTitle = "";
  private String signInSubtitle = "";
  private String registerLabel = "";
  private String footerCopy = "";
  private String termsHref = "";
  private String privacyHref = "";
  private String supportHref = "";
  private List<SlideForm> slides = new ArrayList<>();
  private List<FeatureForm> features = new ArrayList<>();

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

  public List<SlideForm> getSlides() {
    return slides;
  }

  public void setSlides(List<SlideForm> slides) {
    this.slides = slides == null ? new ArrayList<>() : slides;
  }

  public List<FeatureForm> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureForm> features) {
    this.features = features == null ? new ArrayList<>() : features;
  }

  public static class SlideForm {
    private Long id;
    private int sortOrder;
    private boolean enabled;
    private String eyebrow = "";
    private String title = "";
    private String accentPhrase = "";
    private String scriptText = "";
    private String badgeKicker = "";
    private String badgeValue = "";
    private String badgeSub = "";
    private String body = "";
    private String ctaLabel = "";
    private String ctaHref = "";
    private String imageUrl = "";
    private MultipartFile image;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
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

    public String getImageUrl() {
      return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
    }

    public MultipartFile getImage() {
      return image;
    }

    public void setImage(MultipartFile image) {
      this.image = image;
    }
  }

  public static class FeatureForm {
    private Long id;
    private int sortOrder;
    private boolean enabled;
    private String iconKey = "flights";
    private String title = "";
    private String subtitle = "";

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
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
}

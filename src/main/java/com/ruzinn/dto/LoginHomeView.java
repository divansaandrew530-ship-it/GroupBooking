package com.ruzinn.dto;

import com.ruzinn.entity.LoginHomeFeature;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.entity.LoginHomeSlide;
import java.util.List;

public record LoginHomeView(
    String productName,
    String productPill,
    String panelTagline,
    String signInTitle,
    String signInSubtitle,
    String registerLabel,
    String footerCopy,
    String termsHref,
    String privacyHref,
    String supportHref,
    List<SlideView> slides,
    List<FeatureView> features) {

  public static LoginHomeView from(LoginHomePage page) {
    List<SlideView> slides =
        page.getSlides().stream().filter(LoginHomeSlide::isEnabled).map(SlideView::from).toList();
    List<FeatureView> features =
        page.getFeatures().stream().filter(LoginHomeFeature::isEnabled).map(FeatureView::from).toList();
    return new LoginHomeView(
        page.getProductName(),
        page.getProductPill(),
        page.getPanelTagline(),
        page.getSignInTitle(),
        page.getSignInSubtitle(),
        page.getRegisterLabel(),
        page.getFooterCopy(),
        page.getTermsHref(),
        page.getPrivacyHref(),
        page.getSupportHref(),
        slides,
        features);
  }

  public record SlideView(
      Long id,
      String imageUrl,
      String eyebrow,
      String title,
      String accentPhrase,
      String titleBefore,
      String titleAccent,
      String titleAfter,
      String scriptText,
      String badgeKicker,
      String badgeValue,
      String badgeSub,
      String body,
      String ctaLabel,
      String ctaHref) {

    public static SlideView from(LoginHomeSlide slide) {
      TitleParts parts = TitleParts.of(slide.getTitle(), slide.getAccentPhrase());
      return new SlideView(
          slide.getId(),
          "/login-home/media/slides/" + slide.getId(),
          slide.getEyebrow(),
          slide.getTitle(),
          slide.getAccentPhrase(),
          parts.before(),
          parts.accent(),
          parts.after(),
          slide.getScriptText(),
          slide.getBadgeKicker(),
          slide.getBadgeValue(),
          slide.getBadgeSub(),
          slide.getBody(),
          slide.getCtaLabel(),
          slide.getCtaHref() == null || slide.getCtaHref().isBlank() ? "/register" : slide.getCtaHref());
    }

    public boolean hasBadge() {
      return badgeValue != null && !badgeValue.isBlank();
    }
  }

  public record FeatureView(Long id, String iconKey, String title, String subtitle) {
    public static FeatureView from(LoginHomeFeature feature) {
      return new FeatureView(feature.getId(), feature.getIconKey(), feature.getTitle(), feature.getSubtitle());
    }
  }

  public record TitleParts(String before, String accent, String after) {
    public static TitleParts of(String title, String accentPhrase) {
      String text = title == null ? "" : title;
      String accent = accentPhrase == null ? "" : accentPhrase.trim();
      if (accent.isEmpty()) {
        return new TitleParts(text, "", "");
      }
      int index = text.toLowerCase().indexOf(accent.toLowerCase());
      if (index < 0) {
        return new TitleParts(text, "", "");
      }
      return new TitleParts(
          text.substring(0, index),
          text.substring(index, index + accent.length()),
          text.substring(index + accent.length()));
    }
  }
}

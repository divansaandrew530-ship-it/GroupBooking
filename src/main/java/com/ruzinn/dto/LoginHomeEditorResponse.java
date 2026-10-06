package com.ruzinn.dto;

import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.LoginHomeService;
import java.util.List;

public record LoginHomeEditorResponse(
    String lang,
    boolean structureLocked,
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
    List<SlideItem> slides,
    List<FeatureItem> features,
    List<String> iconKeys) {

  public static LoginHomeEditorResponse from(LoginHomeForm form, AppLocale locale) {
    return new LoginHomeEditorResponse(
        locale.code(),
        !locale.english(),
        form.getProductName(),
        form.getProductPill(),
        form.getPanelTagline(),
        form.getSignInTitle(),
        form.getSignInSubtitle(),
        form.getRegisterLabel(),
        form.getFooterCopy(),
        form.getTermsHref(),
        form.getPrivacyHref(),
        form.getSupportHref(),
        slideItems(form),
        featureItems(form),
        LoginHomeService.ICON_KEYS);
  }

  public static List<SlideItem> slideItems(LoginHomeForm form) {
    return form.getSlides().stream().map(SlideItem::from).toList();
  }

  public static List<FeatureItem> featureItems(LoginHomeForm form) {
    return form.getFeatures().stream().map(FeatureItem::from).toList();
  }

  public record SlideItem(
      Long id,
      int sortOrder,
      boolean enabled,
      String eyebrow,
      String title,
      String accentPhrase,
      String scriptText,
      String badgeKicker,
      String badgeValue,
      String badgeSub,
      String body,
      String ctaLabel,
      String ctaHref,
      String imageUrl) {

    public static SlideItem from(LoginHomeForm.SlideForm slide) {
      return new SlideItem(
          slide.getId(),
          slide.getSortOrder(),
          slide.isEnabled(),
          slide.getEyebrow(),
          slide.getTitle(),
          slide.getAccentPhrase(),
          slide.getScriptText(),
          slide.getBadgeKicker(),
          slide.getBadgeValue(),
          slide.getBadgeSub(),
          slide.getBody(),
          slide.getCtaLabel(),
          slide.getCtaHref(),
          slide.getImageUrl());
    }
  }

  public record FeatureItem(
      Long id, int sortOrder, boolean enabled, String iconKey, String title, String subtitle) {
    public static FeatureItem from(LoginHomeForm.FeatureForm feature) {
      return new FeatureItem(
          feature.getId(),
          feature.getSortOrder(),
          feature.isEnabled(),
          feature.getIconKey(),
          feature.getTitle(),
          feature.getSubtitle());
    }
  }
}

package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

/** Mirrors {@link LoginHomeForm}'s text fields; deliberately has no image/file field. */
public record LoginHomeSaveRequest(
    String lang,
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
    List<SlideInput> slides,
    List<FeatureInput> features) {

  public LoginHomeForm toForm() {
    LoginHomeForm form = new LoginHomeForm();
    form.setProductName(productName);
    form.setProductPill(productPill);
    form.setPanelTagline(panelTagline);
    form.setSignInTitle(signInTitle);
    form.setSignInSubtitle(signInSubtitle);
    form.setRegisterLabel(registerLabel);
    form.setFooterCopy(footerCopy);
    form.setTermsHref(termsHref);
    form.setPrivacyHref(privacyHref);
    form.setSupportHref(supportHref);
    List<LoginHomeForm.SlideForm> slideForms = new ArrayList<>();
    if (slides != null) {
      for (SlideInput input : slides) {
        slideForms.add(input.toForm());
      }
    }
    form.setSlides(slideForms);
    List<LoginHomeForm.FeatureForm> featureForms = new ArrayList<>();
    if (features != null) {
      for (FeatureInput input : features) {
        featureForms.add(input.toForm());
      }
    }
    form.setFeatures(featureForms);
    return form;
  }

  public record SlideInput(
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
      String ctaHref) {

    public LoginHomeForm.SlideForm toForm() {
      LoginHomeForm.SlideForm form = new LoginHomeForm.SlideForm();
      form.setId(id);
      form.setSortOrder(sortOrder);
      form.setEnabled(enabled);
      form.setEyebrow(eyebrow);
      form.setTitle(title);
      form.setAccentPhrase(accentPhrase);
      form.setScriptText(scriptText);
      form.setBadgeKicker(badgeKicker);
      form.setBadgeValue(badgeValue);
      form.setBadgeSub(badgeSub);
      form.setBody(body);
      form.setCtaLabel(ctaLabel);
      form.setCtaHref(ctaHref);
      return form;
    }
  }

  public record FeatureInput(
      Long id, int sortOrder, boolean enabled, String iconKey, String title, String subtitle) {

    public LoginHomeForm.FeatureForm toForm() {
      LoginHomeForm.FeatureForm form = new LoginHomeForm.FeatureForm();
      form.setId(id);
      form.setSortOrder(sortOrder);
      form.setEnabled(enabled);
      form.setIconKey(iconKey);
      form.setTitle(title);
      form.setSubtitle(subtitle);
      return form;
    }
  }
}

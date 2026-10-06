package com.ruzinn.serviceimpl;

import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.dto.LoginHomeForm;
import com.ruzinn.dto.LoginHomeView;
import com.ruzinn.entity.CmsTranslation;
import com.ruzinn.entity.LoginHomeFeature;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.entity.LoginHomeSlide;
import com.ruzinn.enums.AppLocale;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.repository.CmsTranslationRepository;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.CmsTranslationService;
import com.ruzinn.service.LoginHomeService;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.CmsLoginHomeCopy;
import com.ruzinn.util.CmsUiCatalog;
import com.ruzinn.util.CmsUiTranslations;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CmsTranslationServiceImpl implements CmsTranslationService {

  private static final Logger log = LoggerFactory.getLogger(CmsTranslationServiceImpl.class);

  private final CmsTranslationRepository translations;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;

  public CmsTranslationServiceImpl(
      CmsTranslationRepository translations,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService) {
    this.translations = translations;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
  }

  @Override
  @Transactional
  public void seedDefaults() {
    seedDefaults(null);
  }

  @Override
  @Transactional
  public void seedDefaults(LoginHomePage page) {
    for (CmsUiCatalog.UiKey key : CmsUiCatalog.keys()) {
      putIfBlank(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key.key(), AppLocale.EN, key.english());
    }
    seedUi(AppLocale.HI, CmsUiTranslations.hindi());
    seedUi(AppLocale.RU, CmsUiTranslations.russian());
    seedUi(AppLocale.AR, CmsUiTranslations.arabic());
    seedLoginHome(AppLocale.HI, CmsLoginHomeCopy.page(AppLocale.HI));
    seedLoginHome(AppLocale.RU, CmsLoginHomeCopy.page(AppLocale.RU));
    seedLoginHome(AppLocale.AR, CmsLoginHomeCopy.page(AppLocale.AR));
    if (page != null) {
      seedSlidesAndFeatures(page);
    }
    refreshQuoteValidityCopy();
    rebrandFooter();
  }

  /** Translated login-home footers saved with the old brand name (WeegooTour) switch to Rathin. */
  private void rebrandFooter() {
    for (AppLocale locale : List.of(AppLocale.HI, AppLocale.RU, AppLocale.AR)) {
      String stored = storedOrEmpty(CmsTranslation.BUNDLE_LOGIN_HOME, CmsTranslation.PAGE_ID, "footer_copy", locale);
      if (stored.toLowerCase(Locale.ROOT).contains("weegootour")) {
        put(
            CmsTranslation.BUNDLE_LOGIN_HOME,
            CmsTranslation.PAGE_ID,
            "footer_copy",
            locale,
            stored.replaceAll("(?i)weegootour", "Rathin"));
        log.info("Login home footer ({}) rebranded to Rathin", locale);
      }
    }
  }

  private void refreshQuoteValidityCopy() {
    refreshUiIfStale(
        "builder.quoteNotBooking",
        AppLocale.EN,
        "This is a quotation, not a confirmed booking. Rates are valid until {0}.",
        "7 days");
    refreshUiIfStale(
        "builder.quoteNotBooking",
        AppLocale.HI,
        "यह कोटेशन है, पुष्टि बुकिंग नहीं। दरें {0} तक मान्य हैं।",
        "7 दिनों");
    refreshUiIfStale(
        "builder.quoteNotBooking",
        AppLocale.RU,
        "Это котировка, а не подтверждённое бронирование. Тарифы действуют до {0}.",
        "7 дней");
    refreshUiIfStale(
        "builder.quoteNotBooking",
        AppLocale.AR,
        "هذا عرض سعر وليس حجزاً مؤكداً. الأسعار سارية حتى {0}.",
        "7 أيام");
  }

  private void refreshUiIfStale(String key, AppLocale locale, String next, String staleMarker) {
    String stored = storedOrEmpty(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key, locale);
    if (stored.isBlank() || !stored.contains(staleMarker) || stored.contains("{0}")) {
      return;
    }
    put(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key, locale, next);
  }

  @Override
  public String ui(String key) {
    return ui(key, AppLocale.current());
  }

  @Override
  public String ui(String key, AppLocale locale) {
    String english = englishUi(key);
    return text(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key, locale, english);
  }

  @Override
  public Map<String, String> uiMap(AppLocale locale) {
    Map<String, String> map = new LinkedHashMap<>();
    for (CmsUiCatalog.UiKey key : CmsUiCatalog.keys()) {
      map.put(key.key(), ui(key.key(), locale));
    }
    return map;
  }

  @Override
  public List<UiRow> uiRows(AppLocale locale) {
    List<UiRow> rows = new ArrayList<>();
    for (CmsUiCatalog.UiKey key : CmsUiCatalog.keys()) {
      rows.add(new UiRow(key.key(), key.group(), key.english(), storedOrEmpty(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key.key(), locale)));
    }
    return rows;
  }

  @Override
  public Map<String, List<UiRow>> uiGroups(AppLocale locale) {
    Map<String, List<UiRow>> groups = new LinkedHashMap<>();
    for (UiRow row : uiRows(locale)) {
      groups.computeIfAbsent(row.group(), key -> new ArrayList<>()).add(row);
    }
    return groups;
  }

  @Override
  @Transactional
  public void saveUi(AppLocale locale, Map<String, String> values) {
    if (values == null) {
      return;
    }
    int saved = 0;
    for (CmsUiCatalog.UiKey key : CmsUiCatalog.keys()) {
      if (!values.containsKey(key.key())) {
        continue;
      }
      put(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, key.key(), locale, values.get(key.key()));
      saved++;
    }
    log.info("UI translations saved for {}: {} key(s)", locale, saved);
  }

  @Override
  public String text(String bundle, String entityId, String field, AppLocale locale, String fallback) {
    AppLocale resolved = locale == null ? AppLocale.EN : locale;
    String stored = storedOrEmpty(bundle, entityId, field, resolved);
    if (!stored.isBlank()) {
      return stored;
    }
    if (!resolved.english()) {
      String english = storedOrEmpty(bundle, entityId, field, AppLocale.EN);
      if (!english.isBlank()) {
        return english;
      }
    }
    return fallback == null ? "" : fallback;
  }

  @Override
  public LoginHomeView localize(LoginHomeView view, AppLocale locale) {
    if (view == null || locale == null || locale.english()) {
      return view;
    }
    String page = CmsTranslation.PAGE_ID;
    List<LoginHomeView.SlideView> slides =
        view.slides() == null
            ? List.of()
            : view.slides().stream().map(slide -> localizeSlide(slide, locale)).toList();
    List<LoginHomeView.FeatureView> features =
        view.features() == null
            ? List.of()
            : view.features().stream().map(feature -> localizeFeature(feature, locale)).toList();
    return new LoginHomeView(
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_name", locale, view.productName()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_pill", locale, view.productPill()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "panel_tagline", locale, view.panelTagline()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_title", locale, view.signInTitle()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_subtitle", locale, view.signInSubtitle()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "register_label", locale, view.registerLabel()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "footer_copy", locale, view.footerCopy()),
        view.termsHref(),
        view.privacyHref(),
        view.supportHref(),
        slides,
        features);
  }

  @Override
  public LoginHomeForm localizeForm(LoginHomeForm form, AppLocale locale) {
    if (form == null || locale == null || locale.english()) {
      return form;
    }
    String page = CmsTranslation.PAGE_ID;
    form.setProductName(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_name", locale, form.getProductName()));
    form.setProductPill(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_pill", locale, form.getProductPill()));
    form.setPanelTagline(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "panel_tagline", locale, form.getPanelTagline()));
    form.setSignInTitle(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_title", locale, form.getSignInTitle()));
    form.setSignInSubtitle(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_subtitle", locale, form.getSignInSubtitle()));
    form.setRegisterLabel(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "register_label", locale, form.getRegisterLabel()));
    form.setFooterCopy(text(CmsTranslation.BUNDLE_LOGIN_HOME, page, "footer_copy", locale, form.getFooterCopy()));
    if (form.getSlides() != null) {
      for (LoginHomeForm.SlideForm slide : form.getSlides()) {
        String id = slideEntity(slide.getId());
        slide.setEyebrow(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "eyebrow", locale, slide.getEyebrow()));
        slide.setTitle(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, slide.getTitle()));
        slide.setAccentPhrase(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "accent_phrase", locale, slide.getAccentPhrase()));
        slide.setScriptText(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "script_text", locale, slide.getScriptText()));
        slide.setBadgeKicker(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_kicker", locale, slide.getBadgeKicker()));
        slide.setBadgeValue(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_value", locale, slide.getBadgeValue()));
        slide.setBadgeSub(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_sub", locale, slide.getBadgeSub()));
        slide.setBody(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "body", locale, slide.getBody()));
        slide.setCtaLabel(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "cta_label", locale, slide.getCtaLabel()));
      }
    }
    if (form.getFeatures() != null) {
      for (LoginHomeForm.FeatureForm feature : form.getFeatures()) {
        String id = featureEntity(feature.getId());
        feature.setTitle(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, feature.getTitle()));
        feature.setSubtitle(text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "subtitle", locale, feature.getSubtitle()));
      }
    }
    return form;
  }

  @Override
  @Transactional
  public void saveLoginHome(LoginHomeForm form, AppLocale locale) {
    if (form == null || locale == null || locale.english()) {
      return;
    }
    String page = CmsTranslation.PAGE_ID;
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_name", locale, form.getProductName());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "product_pill", locale, form.getProductPill());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "panel_tagline", locale, form.getPanelTagline());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_title", locale, form.getSignInTitle());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "sign_in_subtitle", locale, form.getSignInSubtitle());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "register_label", locale, form.getRegisterLabel());
    put(CmsTranslation.BUNDLE_LOGIN_HOME, page, "footer_copy", locale, form.getFooterCopy());
    if (form.getSlides() != null) {
      for (LoginHomeForm.SlideForm slide : form.getSlides()) {
        if (slide.getId() == null) {
          continue;
        }
        String id = slideEntity(slide.getId());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "eyebrow", locale, slide.getEyebrow());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, slide.getTitle());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "accent_phrase", locale, slide.getAccentPhrase());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "script_text", locale, slide.getScriptText());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_kicker", locale, slide.getBadgeKicker());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_value", locale, slide.getBadgeValue());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_sub", locale, slide.getBadgeSub());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "body", locale, slide.getBody());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "cta_label", locale, slide.getCtaLabel());
      }
    }
    if (form.getFeatures() != null) {
      for (LoginHomeForm.FeatureForm feature : form.getFeatures()) {
        if (feature.getId() == null) {
          continue;
        }
        String id = featureEntity(feature.getId());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, feature.getTitle());
        put(CmsTranslation.BUNDLE_LOGIN_HOME, id, "subtitle", locale, feature.getSubtitle());
      }
    }
    log.info("Login home translation saved for {}", locale);
  }

  @Override
  public void applyItemForm(ItemMasterForm form, AppLocale locale) {
    if (form == null || locale == null || locale.english() || form.getItemId() == null || form.getItemId().isBlank()) {
      return;
    }
    String id = form.getItemId();
    form.setItemName(text(CmsTranslation.BUNDLE_ITEM_MASTER, id, "item_name", locale, form.getItemName()));
    form.setDescription(text(CmsTranslation.BUNDLE_ITEM_MASTER, id, "description", locale, form.getDescription()));
    form.setTermsAndCondition(
        text(CmsTranslation.BUNDLE_ITEM_MASTER, id, "terms_and_condition", locale, form.getTermsAndCondition()));
    form.setIncluded(text(CmsTranslation.BUNDLE_ITEM_MASTER, id, "included", locale, form.getIncluded()));
    form.setExcluded(text(CmsTranslation.BUNDLE_ITEM_MASTER, id, "excluded", locale, form.getExcluded()));
  }

  @Override
  @Transactional
  public void saveItemCopy(String itemId, ItemMasterForm form, AppLocale locale) {
    if (itemId == null || itemId.isBlank() || form == null || locale == null || locale.english()) {
      return;
    }
    put(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, "item_name", locale, form.getItemName());
    put(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, "description", locale, form.getDescription());
    put(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, "terms_and_condition", locale, form.getTermsAndCondition());
    put(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, "included", locale, form.getIncluded());
    put(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, "excluded", locale, form.getExcluded());
    log.info("Item {} translation saved for {}", itemId, locale);
  }

  @Override
  public String itemField(String itemId, String field, String fallback) {
    return text(CmsTranslation.BUNDLE_ITEM_MASTER, itemId, field, AppLocale.current(), fallback);
  }

  @Override
  @Transactional
  public void put(String bundle, String entityId, String field, AppLocale locale, String value) {
    if (locale == null || field == null || field.isBlank()) {
      return;
    }
    String text = value == null ? "" : value;
    CmsTranslation row =
        translations
            .findByBundleAndEntityIdAndFieldNameAndLocale(bundle, entity(entityId), field, locale.code())
            .orElseGet(CmsTranslation::new);
    boolean creating = row.getId() == null;
    String previous = creating ? "" : row.getTextValue();
    if (!creating && text.equals(previous == null ? "" : previous)) {
      return;
    }
    if (!creating) {
      rowHistoryService.archive(
          EditableTable.CMS_TRANSLATION, row.getId(), ChangeHistoryService.CMS_SAVE, field);
    }
    row.setBundle(bundle);
    row.setEntityId(entity(entityId));
    row.setFieldName(field);
    row.setLocale(locale.code());
    row.setTextValue(text);
    row.setUpdatedDate(Instant.now());
    translations.save(row);
    changeHistoryService.record(
        ChangeHistoryService.CMS_TRANSLATION,
        row.getBundle() + ":" + row.getEntityId() + ":" + row.getFieldName() + ":" + row.getLocale(),
        row.getLocale(),
        ChangeHistoryService.CMS_SAVE,
        field,
        previous,
        text,
        null,
        null);
  }

  private void putIfBlank(String bundle, String entityId, String field, AppLocale locale, String value) {
    String stored = storedOrEmpty(bundle, entityId, field, locale);
    if (stored.isBlank() && value != null && !value.isBlank()) {
      put(bundle, entityId, field, locale, value);
    }
  }

  private void seedUi(AppLocale locale, Map<String, String> values) {
    for (Map.Entry<String, String> entry : values.entrySet()) {
      putIfBlank(CmsTranslation.BUNDLE_UI, CmsTranslation.UI_ENTITY, entry.getKey(), locale, entry.getValue());
    }
  }

  private void seedLoginHome(AppLocale locale, Map<String, String> values) {
    for (Map.Entry<String, String> entry : values.entrySet()) {
      putIfBlank(CmsTranslation.BUNDLE_LOGIN_HOME, CmsTranslation.PAGE_ID, entry.getKey(), locale, entry.getValue());
    }
  }

  private void seedSlidesAndFeatures(LoginHomePage page) {
    if (page.getSlides() != null) {
      for (LoginHomeSlide slide : page.getSlides()) {
        if (slide.getId() == null) {
          continue;
        }
        String id = slideEntity(slide.getId());
        for (AppLocale locale : List.of(AppLocale.HI, AppLocale.RU, AppLocale.AR)) {
          for (Map.Entry<String, String> entry : CmsLoginHomeCopy.slide(slide.getSortOrder(), locale).entrySet()) {
            putIfBlank(CmsTranslation.BUNDLE_LOGIN_HOME, id, entry.getKey(), locale, entry.getValue());
          }
        }
      }
    }
    if (page.getFeatures() != null) {
      boolean uniqueIcons = uniqueFeatureIcons(page.getFeatures());
      for (LoginHomeFeature feature : page.getFeatures()) {
        if (feature.getId() == null) {
          continue;
        }
        String id = featureEntity(feature.getId());
        String copyKey = featureCopyKey(feature, uniqueIcons);
        for (AppLocale locale : List.of(AppLocale.HI, AppLocale.RU, AppLocale.AR)) {
          Map<String, String> copy = CmsLoginHomeCopy.feature(copyKey, locale);
          Map<String, String> flightsCopy = CmsLoginHomeCopy.feature("flights", locale);
          for (Map.Entry<String, String> entry : copy.entrySet()) {
            seedFeatureField(
                id, entry.getKey(), locale, entry.getValue(), flightsCopy.get(entry.getKey()), copyKey);
          }
        }
      }
    }
  }

  private static boolean uniqueFeatureIcons(List<LoginHomeFeature> features) {
    Set<String> keys = new HashSet<>();
    for (LoginHomeFeature feature : features) {
      String icon = feature.getIconKey() == null ? "" : feature.getIconKey();
      if (!keys.add(icon)) {
        return false;
      }
    }
    return true;
  }

  private static String featureCopyKey(LoginHomeFeature feature, boolean uniqueIcons) {
    String title = feature.getTitle() == null ? "" : feature.getTitle().toLowerCase(Locale.ROOT);
    if (title.contains("hotel")) {
      return "hotels";
    }
    if (title.contains("attraction")) {
      return "attractions";
    }
    if (title.contains("support")) {
      return "support";
    }
    if (title.contains("flight")) {
      return "flights";
    }
    if (uniqueIcons && feature.getIconKey() != null && !feature.getIconKey().isBlank()) {
      return feature.getIconKey();
    }
    int idx = Math.max(feature.getSortOrder(), 1) - 1;
    if (idx < LoginHomeService.ICON_KEYS.size()) {
      return LoginHomeService.ICON_KEYS.get(idx);
    }
    return feature.getIconKey();
  }

  private void seedFeatureField(
      String entityId, String field, AppLocale locale, String wanted, String flightsDefault, String copyKey) {
    String stored = storedOrEmpty(CmsTranslation.BUNDLE_LOGIN_HOME, entityId, field, locale);
    if (stored.isBlank()) {
      putIfBlank(CmsTranslation.BUNDLE_LOGIN_HOME, entityId, field, locale, wanted);
      return;
    }
    if (wanted != null
        && !wanted.equals(stored)
        && flightsDefault != null
        && flightsDefault.equals(stored)
        && !"flights".equals(copyKey)) {
      put(CmsTranslation.BUNDLE_LOGIN_HOME, entityId, field, locale, wanted);
    }
  }

  private LoginHomeView.SlideView localizeSlide(LoginHomeView.SlideView slide, AppLocale locale) {
    String id = slideEntity(slide.id());
    String title = text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, slide.title());
    String accent = text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "accent_phrase", locale, slide.accentPhrase());
    LoginHomeView.TitleParts parts = LoginHomeView.TitleParts.of(title, accent);
    return new LoginHomeView.SlideView(
        slide.id(),
        slide.imageUrl(),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "eyebrow", locale, slide.eyebrow()),
        title,
        accent,
        parts.before(),
        parts.accent(),
        parts.after(),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "script_text", locale, slide.scriptText()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_kicker", locale, slide.badgeKicker()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_value", locale, slide.badgeValue()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "badge_sub", locale, slide.badgeSub()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "body", locale, slide.body()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "cta_label", locale, slide.ctaLabel()),
        slide.ctaHref());
  }

  private LoginHomeView.FeatureView localizeFeature(LoginHomeView.FeatureView feature, AppLocale locale) {
    String id = featureEntity(feature.id());
    return new LoginHomeView.FeatureView(
        feature.id(),
        feature.iconKey(),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "title", locale, feature.title()),
        text(CmsTranslation.BUNDLE_LOGIN_HOME, id, "subtitle", locale, feature.subtitle()));
  }

  private String storedOrEmpty(String bundle, String entityId, String field, AppLocale locale) {
    return translations
        .findByBundleAndEntityIdAndFieldNameAndLocale(bundle, entity(entityId), field, locale.code())
        .map(CmsTranslation::getTextValue)
        .orElse("");
  }

  private static String entity(String entityId) {
    return entityId == null || entityId.isBlank() ? CmsTranslation.UI_ENTITY : entityId;
  }

  private static String slideEntity(Long id) {
    return id == null ? CmsTranslation.UI_ENTITY : "slide-" + id;
  }

  private static String featureEntity(Long id) {
    return id == null ? CmsTranslation.UI_ENTITY : "feature-" + id;
  }

  private static String englishUi(String key) {
    return CmsUiCatalog.keys().stream()
        .filter(item -> item.key().equals(key))
        .map(CmsUiCatalog.UiKey::english)
        .findFirst()
        .orElse(key);
  }
}

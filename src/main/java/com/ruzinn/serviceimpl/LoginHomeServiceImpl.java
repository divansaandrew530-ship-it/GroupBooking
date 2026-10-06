package com.ruzinn.serviceimpl;

import com.ruzinn.dto.LoginHomeForm;
import com.ruzinn.dto.LoginHomeView;
import com.ruzinn.entity.LoginHomeFeature;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.entity.LoginHomeSlide;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.repository.LoginHomePageRepository;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.LoginHomeMediaStore;
import com.ruzinn.service.LoginHomeService;
import com.ruzinn.service.RowHistoryService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LoginHomeServiceImpl implements LoginHomeService {

  private static final Logger log = LoggerFactory.getLogger(LoginHomeServiceImpl.class);

  private final LoginHomePageRepository pages;
  private final LoginHomeMediaStore mediaStore;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;

  public LoginHomeServiceImpl(
      LoginHomePageRepository pages,
      LoginHomeMediaStore mediaStore,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService) {
    this.pages = pages;
    this.mediaStore = mediaStore;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
  }

  @Override
  @Transactional
  public LoginHomePage ensureDefaults() {
    LoginHomePage page = pages.findById(LoginHomePage.SINGLETON_ID).orElseGet(this::createPage);
    boolean changed = false;
    if (page.getSlides().isEmpty()) {
      addDefaultSlide(
          page,
          1,
          "GROUP BOOKINGS MADE SIMPLE",
          "Big Savings for Bigger Journeys",
          "for Bigger",
          "Travel More Together",
          "UP TO",
          "30% OFF",
          "GROUP BOOKINGS",
          "Exclusive group fares on flights, hotels and activities worldwide.",
          "classpath:images/login/slide-1.jpg");
      addDefaultSlide(
          page,
          2,
          "HOTELS FOR EVERY GROUP",
          "Stay Together for Less",
          "for Less",
          "Better Together",
          "UP TO",
          "25% OFF",
          "GROUP STAYS",
          "Block rooms at special group rates across city and resort hotels.",
          "classpath:images/login/slide-2.jpg");
      addDefaultSlide(
          page,
          3,
          "ATTRACTIONS AND TOURS",
          "See More as a Group",
          "as a Group",
          "Travel More Together",
          "UP TO",
          "20% OFF",
          "GROUP TICKETS",
          "Parks, city tours, and local attractions with dedicated group slots.",
          "classpath:images/login/slide-3.jpg");
      addDefaultSlide(
          page,
          4,
          "GROUP FLIGHT FARES",
          "Fly Together and Save",
          "and Save",
          "Greater Possibilities",
          "UP TO",
          "15% OFF",
          "GROUP FLIGHTS",
          "Coordinated group airfares so every passenger travels on the same plan.",
          "classpath:images/login/slide-4.jpg");
      changed = true;
    }
    if (page.getFeatures().isEmpty()) {
      addDefaultFeature(page, 1, "flights", "Group Flights", "Better fares");
      addDefaultFeature(page, 2, "hotels", "Group Hotels", "Special rates");
      addDefaultFeature(page, 3, "attractions", "Local Attractions", "Group discounts");
      addDefaultFeature(page, 4, "support", "Dedicated Support", "For your business");
      changed = true;
    }
    String footer = page.getFooterCopy();
    if (footer != null && footer.toLowerCase().contains("weegootour")) {
      // Old brand name saved in the database: replace it once.
      page.setFooterCopy(footer.replaceAll("(?i)weegootour", "Rathin"));
      log.info("Login home footer rebranded: '{}' -> '{}'", footer, page.getFooterCopy());
      changed = true;
    }
    return changed ? pages.save(page) : page;
  }

  @Override
  @Transactional
  public LoginHomeView published() {
    return LoginHomeView.from(ensureLoaded());
  }

  @Override
  @Transactional
  public LoginHomeForm editorForm() {
    return toForm(ensureLoaded());
  }

  @Override
  @Transactional
  public void save(LoginHomeForm form) {
    LoginHomePage page = ensureLoaded();
    archiveLoginHome(page, ChangeHistoryService.CMS_SAVE);
    recordPageField("product_name", page.getProductName(), required(form.getProductName(), "Product name"));
    recordPageField("product_pill", page.getProductPill(), blankToEmpty(form.getProductPill()));
    recordPageField("panel_tagline", page.getPanelTagline(), blankToEmpty(form.getPanelTagline()));
    recordPageField("sign_in_title", page.getSignInTitle(), required(form.getSignInTitle(), "Sign in title"));
    recordPageField("sign_in_subtitle", page.getSignInSubtitle(), blankToEmpty(form.getSignInSubtitle()));
    String registerLabel =
        blankToEmpty(form.getRegisterLabel()).isBlank() ? "Register" : form.getRegisterLabel().trim();
    recordPageField("register_label", page.getRegisterLabel(), registerLabel);
    recordPageField("footer_copy", page.getFooterCopy(), blankToEmpty(form.getFooterCopy()));
    recordPageField("terms_href", page.getTermsHref(), href(form.getTermsHref()));
    recordPageField("privacy_href", page.getPrivacyHref(), href(form.getPrivacyHref()));
    recordPageField("support_href", page.getSupportHref(), href(form.getSupportHref()));
    page.setProductName(required(form.getProductName(), "Product name"));
    page.setProductPill(blankToEmpty(form.getProductPill()));
    page.setPanelTagline(blankToEmpty(form.getPanelTagline()));
    page.setSignInTitle(required(form.getSignInTitle(), "Sign in title"));
    page.setSignInSubtitle(blankToEmpty(form.getSignInSubtitle()));
    page.setRegisterLabel(registerLabel);
    page.setFooterCopy(blankToEmpty(form.getFooterCopy()));
    page.setTermsHref(href(form.getTermsHref()));
    page.setPrivacyHref(href(form.getPrivacyHref()));
    page.setSupportHref(href(form.getSupportHref()));
    page.setUpdatedDate(Instant.now());

    Map<Long, LoginHomeSlide> slidesById =
        page.getSlides().stream()
            .filter(slide -> slide.getId() != null)
            .collect(Collectors.toMap(LoginHomeSlide::getId, Function.identity()));
    int order = 1;
    for (LoginHomeForm.SlideForm item : form.getSlides() == null ? List.<LoginHomeForm.SlideForm>of() : form.getSlides()) {
      if (item.getId() == null) {
        continue;
      }
      LoginHomeSlide slide = slidesById.get(item.getId());
      if (slide == null) {
        continue;
      }
      String previousTitle = slide.getTitle();
      slide.setSortOrder(item.getSortOrder() > 0 ? item.getSortOrder() : order);
      slide.setEnabled(item.isEnabled());
      slide.setEyebrow(blankToEmpty(item.getEyebrow()));
      slide.setTitle(required(item.getTitle(), "Slide title"));
      slide.setAccentPhrase(blankToEmpty(item.getAccentPhrase()));
      slide.setScriptText(blankToEmpty(item.getScriptText()));
      slide.setBadgeKicker(blankToEmpty(item.getBadgeKicker()));
      slide.setBadgeValue(blankToEmpty(item.getBadgeValue()));
      slide.setBadgeSub(blankToEmpty(item.getBadgeSub()));
      slide.setBody(blankToEmpty(item.getBody()));
      slide.setCtaLabel(blankToEmpty(item.getCtaLabel()).isBlank() ? "Book Group Deals" : item.getCtaLabel().trim());
      slide.setCtaHref(href(item.getCtaHref()));
      storeImage(slide, item.getImage());
      recordPageField("slide_title", previousTitle, slide.getTitle());
      order++;
    }

    Map<Long, LoginHomeFeature> featuresById =
        page.getFeatures().stream()
            .filter(feature -> feature.getId() != null)
            .collect(Collectors.toMap(LoginHomeFeature::getId, Function.identity()));
    order = 1;
    for (LoginHomeForm.FeatureForm item :
        form.getFeatures() == null ? List.<LoginHomeForm.FeatureForm>of() : form.getFeatures()) {
      if (item.getId() == null) {
        continue;
      }
      LoginHomeFeature feature = featuresById.get(item.getId());
      if (feature == null) {
        continue;
      }
      feature.setSortOrder(item.getSortOrder() > 0 ? item.getSortOrder() : order);
      feature.setEnabled(item.isEnabled());
      feature.setIconKey(normalizeIcon(item.getIconKey()));
      feature.setTitle(required(item.getTitle(), "Feature title"));
      feature.setSubtitle(blankToEmpty(item.getSubtitle()));
      order++;
    }
    pages.save(page);
    log.info(
        "Login home saved: {} slide(s), {} feature(s)", page.getSlides().size(), page.getFeatures().size());
  }

  @Override
  @Transactional
  public void addSlide() {
    LoginHomePage page = ensureLoaded();
    int next =
        page.getSlides().stream().mapToInt(LoginHomeSlide::getSortOrder).max().orElse(0) + 1;
    addDefaultSlide(
        page,
        next,
        "NEW GROUP OFFER",
        "Add a headline for this slide",
        "headline",
        "Travel More Together",
        "UP TO",
        "10% OFF",
        "GROUP BOOKINGS",
        "Describe this offer for company group bookings.",
        defaultImagePath(next));
    pages.save(page);
    changeHistoryService.record(
        ChangeHistoryService.LOGIN_HOME,
        String.valueOf(LoginHomePage.SINGLETON_ID),
        null,
        ChangeHistoryService.SLIDE_ADD,
        "slide_title",
        "",
        "Add a headline for this slide",
        "1",
        "Rathin");
    log.info("Login home slide added at position {} ({} slide(s) now)", next, page.getSlides().size());
  }

  @Override
  @Transactional
  public void deleteSlide(Long slideId) {
    LoginHomePage page = ensureLoaded();
    if (page.getSlides().size() <= 1) {
      throw new IllegalArgumentException("Keep at least one slide on the login home.");
    }
    page.getSlides().stream()
        .filter(slide -> slideId.equals(slide.getId()))
        .findFirst()
        .ifPresent(
            slide ->
                rowHistoryService.archive(
                    EditableTable.LOGIN_HOME_SLIDE,
                    slide.getId(),
                    ChangeHistoryService.SLIDE_DELETE,
                    "login_home_slide"));
    page.getSlides().removeIf(slide -> slideId.equals(slide.getId()));
    pages.save(page);
    changeHistoryService.record(
        ChangeHistoryService.LOGIN_HOME,
        String.valueOf(slideId),
        null,
        ChangeHistoryService.SLIDE_DELETE,
        "slide_title",
        String.valueOf(slideId),
        "",
        "1",
        "Rathin");
    log.info("Login home slide {} deleted ({} slide(s) left)", slideId, page.getSlides().size());
  }

  @Override
  @Transactional
  public LoginHomeSlide uploadSlideImage(Long slideId, MultipartFile file) {
    LoginHomePage page = ensureLoaded();
    LoginHomeSlide slide =
        page.getSlides().stream()
            .filter(item -> slideId.equals(item.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Slide was not found"));
    storeImage(slide, file);
    pages.save(page);
    log.info(
        "Login home slide {} image uploaded ({}, {} bytes)",
        slideId, file == null ? "-" : file.getOriginalFilename(), file == null ? 0 : file.getSize());
    return slide;
  }

  @Override
  @Transactional(readOnly = true)
  public MediaResource slideImage(Long slideId) {
    LoginHomePage page = ensureLoaded();
    LoginHomeSlide slide =
        page.getSlides().stream()
            .filter(item -> slideId.equals(item.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Slide was not found"));
    String path = slide.getImagePath();
    if (mediaStore.existsUploaded(path)) {
      return new MediaResource(
          mediaStore.readUploaded(path),
          contentType(slide.getImageContentType(), path));
    }
    String fallback = path != null && path.startsWith("classpath:") ? path : defaultImagePath(slide.getSortOrder());
    if (mediaStore.existsClasspath(fallback)) {
      return new MediaResource(mediaStore.readClasspath(fallback), MediaType.IMAGE_JPEG_VALUE);
    }
    throw new IllegalArgumentException("Slide image was not found");
  }

  private LoginHomePage createPage() {
    LoginHomePage page = new LoginHomePage();
    page.setId(LoginHomePage.SINGLETON_ID);
    page.setUpdatedDate(Instant.now());
    return pages.save(page);
  }

  private LoginHomePage ensureLoaded() {
    LoginHomePage page = ensureDefaults();
    page.getSlides().size();
    page.getFeatures().size();
    page.getSlides().sort(Comparator.comparingInt(LoginHomeSlide::getSortOrder).thenComparing(slide -> slide.getId() == null ? 0L : slide.getId()));
    page.getFeatures()
        .sort(
            Comparator.comparingInt(LoginHomeFeature::getSortOrder)
                .thenComparing(feature -> feature.getId() == null ? 0L : feature.getId()));
    return page;
  }

  private static LoginHomeForm toForm(LoginHomePage page) {
    LoginHomeForm form = new LoginHomeForm();
    form.setProductName(page.getProductName());
    form.setProductPill(page.getProductPill());
    form.setPanelTagline(page.getPanelTagline());
    form.setSignInTitle(page.getSignInTitle());
    form.setSignInSubtitle(page.getSignInSubtitle());
    form.setRegisterLabel(page.getRegisterLabel());
    form.setFooterCopy(page.getFooterCopy());
    form.setTermsHref(page.getTermsHref());
    form.setPrivacyHref(page.getPrivacyHref());
    form.setSupportHref(page.getSupportHref());
    for (LoginHomeSlide slide : page.getSlides()) {
      LoginHomeForm.SlideForm item = new LoginHomeForm.SlideForm();
      item.setId(slide.getId());
      item.setSortOrder(slide.getSortOrder());
      item.setEnabled(slide.isEnabled());
      item.setEyebrow(slide.getEyebrow());
      item.setTitle(slide.getTitle());
      item.setAccentPhrase(slide.getAccentPhrase());
      item.setScriptText(slide.getScriptText());
      item.setBadgeKicker(slide.getBadgeKicker());
      item.setBadgeValue(slide.getBadgeValue());
      item.setBadgeSub(slide.getBadgeSub());
      item.setBody(slide.getBody());
      item.setCtaLabel(slide.getCtaLabel());
      item.setCtaHref(slide.getCtaHref());
      item.setImageUrl("/login-home/media/slides/" + slide.getId());
      form.getSlides().add(item);
    }
    for (LoginHomeFeature feature : page.getFeatures()) {
      LoginHomeForm.FeatureForm item = new LoginHomeForm.FeatureForm();
      item.setId(feature.getId());
      item.setSortOrder(feature.getSortOrder());
      item.setEnabled(feature.isEnabled());
      item.setIconKey(feature.getIconKey());
      item.setTitle(feature.getTitle());
      item.setSubtitle(feature.getSubtitle());
      form.getFeatures().add(item);
    }
    return form;
  }

  private void archiveLoginHome(LoginHomePage page, String action) {
    rowHistoryService.archive(EditableTable.LOGIN_HOME_PAGE, page.getId(), action, "login_home_page");
    for (LoginHomeSlide slide : page.getSlides()) {
      if (slide.getId() != null) {
        rowHistoryService.archive(EditableTable.LOGIN_HOME_SLIDE, slide.getId(), action, "login_home_slide");
      }
    }
    for (LoginHomeFeature feature : page.getFeatures()) {
      if (feature.getId() != null) {
        rowHistoryService.archive(
            EditableTable.LOGIN_HOME_FEATURE, feature.getId(), action, "login_home_feature");
      }
    }
  }

  private void recordPageField(String fieldName, String oldValue, String newValue) {
    changeHistoryService.record(
        ChangeHistoryService.LOGIN_HOME,
        String.valueOf(LoginHomePage.SINGLETON_ID),
        null,
        ChangeHistoryService.CMS_SAVE,
        fieldName,
        oldValue,
        newValue,
        "1",
        "Rathin");
  }

  private void storeImage(LoginHomeSlide slide, MultipartFile image) {
    if (image == null || image.isEmpty()) {
      return;
    }
    String stored = mediaStore.saveSlide(slide.getId(), image);
    if (stored != null) {
      slide.setImagePath(stored);
      slide.setImageContentType(
          image.getContentType() == null || image.getContentType().isBlank()
              ? "image/jpeg"
              : image.getContentType());
    }
  }

  private static void addDefaultSlide(
      LoginHomePage page,
      int order,
      String eyebrow,
      String title,
      String accent,
      String script,
      String badgeKicker,
      String badgeValue,
      String badgeSub,
      String body,
      String imagePath) {
    LoginHomeSlide slide = new LoginHomeSlide();
    slide.setSortOrder(order);
    slide.setEnabled(true);
    slide.setEyebrow(eyebrow);
    slide.setTitle(title);
    slide.setAccentPhrase(accent);
    slide.setScriptText(script);
    slide.setBadgeKicker(badgeKicker);
    slide.setBadgeValue(badgeValue);
    slide.setBadgeSub(badgeSub);
    slide.setBody(body);
    slide.setCtaLabel("Book Group Deals");
    slide.setCtaHref("/register");
    slide.setImagePath(imagePath);
    slide.setImageContentType("image/jpeg");
    page.addSlide(slide);
  }

  private static void addDefaultFeature(
      LoginHomePage page, int order, String icon, String title, String subtitle) {
    LoginHomeFeature feature = new LoginHomeFeature();
    feature.setSortOrder(order);
    feature.setEnabled(true);
    feature.setIconKey(icon);
    feature.setTitle(title);
    feature.setSubtitle(subtitle);
    page.addFeature(feature);
  }

  private static String defaultImagePath(int order) {
    int index = Math.min(Math.max(order, 1), 4);
    return "classpath:images/login/slide-" + index + ".jpg";
  }

  private static String required(String value, String label) {
    if (value == null || value.trim().isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String href(String value) {
    String href = blankToEmpty(value);
    if (href.isBlank()) {
      return "#";
    }
    if (href.startsWith("/") || href.startsWith("#") || href.startsWith("http://") || href.startsWith("https://")
        || href.startsWith("mailto:")) {
      return href;
    }
    throw new IllegalArgumentException("Links must start with /, #, mailto:, or https://");
  }

  private static String normalizeIcon(String iconKey) {
    String key = blankToEmpty(iconKey).toLowerCase();
    return ICON_KEYS.contains(key) ? key : "flights";
  }

  private static String contentType(String stored, String path) {
    if (stored != null && !stored.isBlank()) {
      return stored;
    }
    if (path != null && path.endsWith(".png")) {
      return MediaType.IMAGE_PNG_VALUE;
    }
    if (path != null && path.endsWith(".webp")) {
      return "image/webp";
    }
    return MediaType.IMAGE_JPEG_VALUE;
  }
}

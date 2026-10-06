package com.ruzinn.controller;

import com.ruzinn.dto.LoginHomeEditorResponse;
import com.ruzinn.dto.LoginHomeForm;
import com.ruzinn.dto.LoginHomeSaveRequest;
import com.ruzinn.dto.LoginHomeView;
import com.ruzinn.entity.LoginHomeSlide;
import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.CmsTranslationService;
import com.ruzinn.service.LoginHomeService;
import com.ruzinn.util.FileResponses;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class LoginHomeController {

  private final LoginHomeService loginHomeService;
  private final CmsTranslationService translations;

  public LoginHomeController(LoginHomeService loginHomeService, CmsTranslationService translations) {
    this.loginHomeService = loginHomeService;
    this.translations = translations;
  }

  /** Published login-home content for the public sign-in screen. */
  @GetMapping("/api/login-home")
  public LoginHomeView published(@RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = lang == null ? AppLocale.current() : AppLocale.from(lang);
    return translations.localize(loginHomeService.published(), locale);
  }

  @GetMapping("/api/admin/login-home")
  public LoginHomeEditorResponse editor(@RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = AppLocale.from(lang == null ? AppLocale.current().code() : lang);
    LoginHomeForm form = translations.localizeForm(loginHomeService.editorForm(), locale);
    return LoginHomeEditorResponse.from(form, locale);
  }

  @PostMapping("/api/admin/login-home")
  public LoginHomeEditorResponse save(@RequestBody LoginHomeSaveRequest request) {
    AppLocale locale = AppLocale.from(request == null ? null : request.lang());
    LoginHomeForm form = request == null ? new LoginHomeForm() : request.toForm();
    if (locale.english()) {
      loginHomeService.save(form);
    } else {
      translations.saveLoginHome(form, locale);
    }
    LoginHomeForm updated = translations.localizeForm(loginHomeService.editorForm(), locale);
    return LoginHomeEditorResponse.from(updated, locale);
  }

  @PostMapping("/api/admin/login-home/slides")
  public List<LoginHomeEditorResponse.SlideItem> addSlide(
      @RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = AppLocale.from(lang);
    loginHomeService.addSlide();
    LoginHomeForm form = translations.localizeForm(loginHomeService.editorForm(), locale);
    return LoginHomeEditorResponse.slideItems(form);
  }

  /** Angular admin: {@code DELETE /slides/{id}} (same as the POST form below). */
  @DeleteMapping("/api/admin/login-home/slides/{slideId}")
  public List<LoginHomeEditorResponse.SlideItem> removeSlide(
      @PathVariable Long slideId, @RequestParam(name = "lang", required = false) String lang) {
    return deleteSlide(slideId, lang);
  }

  @PostMapping("/api/admin/login-home/slides/{slideId}/delete")
  public List<LoginHomeEditorResponse.SlideItem> deleteSlide(
      @PathVariable Long slideId, @RequestParam(name = "lang", required = false) String lang) {
    AppLocale locale = AppLocale.from(lang);
    loginHomeService.deleteSlide(slideId);
    LoginHomeForm form = translations.localizeForm(loginHomeService.editorForm(), locale);
    return LoginHomeEditorResponse.slideItems(form);
  }

  @PostMapping("/api/admin/login-home/slides/{slideId}/image")
  public Map<String, Object> uploadSlideImage(
      @PathVariable Long slideId, @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Choose an image to upload");
    }
    LoginHomeSlide slide = loginHomeService.uploadSlideImage(slideId, file);
    return Map.of("id", slide.getId(), "imageUrl", "/login-home/media/slides/" + slide.getId());
  }

  @GetMapping("/login-home/media/slides/{slideId}")
  public ResponseEntity<byte[]> slideImage(@PathVariable Long slideId) {
    try {
      LoginHomeService.MediaResource media = loginHomeService.slideImage(slideId);
      return FileResponses.image(media.contentType(), media.bytes());
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }
}

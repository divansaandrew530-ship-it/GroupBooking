package com.ruzinn.controller;

import com.ruzinn.dto.ItemImportResult;
import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.dto.ItemMasterView;
import com.ruzinn.enums.AppLocale;
import com.ruzinn.service.CmsTranslationService;
import com.ruzinn.service.ItemMasterService;
import com.ruzinn.util.FileResponses;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ItemMasterController {

  private final ItemMasterService itemService;
  private final CmsTranslationService translations;

  public ItemMasterController(ItemMasterService itemService, CmsTranslationService translations) {
    this.itemService = itemService;
    this.translations = translations;
  }

  @GetMapping("/api/admin/items")
  public List<ItemMasterView> list(@RequestParam(required = false) String q, HttpSession session) {
    return itemService.list(SessionUser.current(session), q);
  }

  @GetMapping("/api/admin/items/template")
  public ResponseEntity<byte[]> template(HttpSession session) {
    byte[] bytes = itemService.template(SessionUser.current(session));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(
        ContentDisposition.attachment().filename("park-items-template.xlsx", StandardCharsets.UTF_8).build());
    return ResponseEntity.ok()
        .headers(headers)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @GetMapping("/api/admin/items/{itemId}")
  public ItemMasterView get(@PathVariable String itemId, HttpSession session) {
    return itemService.get(SessionUser.current(session), itemId);
  }

  @PostMapping("/api/admin/items")
  public ItemMasterView create(@RequestBody ItemMasterForm form, HttpSession session) {
    form.setItemId("");
    return itemService.save(SessionUser.current(session), form);
  }

  @PutMapping("/api/admin/items/{itemId}")
  public ItemMasterView update(
      @PathVariable String itemId, @RequestBody ItemMasterForm form, HttpSession session) {
    form.setItemId(itemId);
    return itemService.save(SessionUser.current(session), form);
  }

  /** Item text in one language; blank translated fields fall back to English. */
  @GetMapping("/api/admin/items/{itemId}/translations/{lang}")
  public ItemMasterForm translation(
      @PathVariable String itemId, @PathVariable String lang, HttpSession session) {
    ItemMasterForm form = itemService.form(SessionUser.current(session), itemId);
    translations.applyItemForm(form, AppLocale.from(lang));
    return form;
  }

  /** Saves translated item text; for English this updates the item itself. */
  @PutMapping("/api/admin/items/{itemId}/translations/{lang}")
  public ItemMasterForm saveTranslation(
      @PathVariable String itemId,
      @PathVariable String lang,
      @RequestBody ItemMasterForm form,
      HttpSession session) {
    AppLocale locale = AppLocale.from(lang);
    form.setItemId(itemId);
    if (locale.english()) {
      itemService.save(SessionUser.current(session), form);
    } else {
      itemService.form(SessionUser.current(session), itemId);
      translations.saveItemCopy(itemId, form, locale);
    }
    return translation(itemId, lang, session);
  }

  @PostMapping(value = "/api/admin/items/{itemId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ItemMasterView uploadImages(
      @PathVariable String itemId,
      @RequestParam(name = "imagePdfFile", required = false) MultipartFile imagePdfFile,
      @RequestParam(name = "imageTicketFile", required = false) MultipartFile imageTicketFile,
      HttpSession session) {
    ItemMasterForm form = itemService.form(SessionUser.current(session), itemId);
    form.setImagePdfFile(imagePdfFile);
    form.setImageTicketFile(imageTicketFile);
    return itemService.save(SessionUser.current(session), form);
  }

  @DeleteMapping("/api/admin/items/{itemId}")
  public java.util.Map<String, String> delete(@PathVariable String itemId, HttpSession session) {
    itemService.delete(SessionUser.current(session), itemId);
    return java.util.Map.of("status", "deleted");
  }

  @PostMapping("/api/admin/items/upload")
  public ItemImportResult upload(@RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Choose an Excel file to upload");
    }
    return itemService.importExcel(SessionUser.current(session), file.getInputStream());
  }

  @GetMapping("/admin/items/{itemId}/image/{kind}")
  public ResponseEntity<byte[]> image(
      @PathVariable String itemId, @PathVariable String kind, HttpSession session) {
    try {
      var media = itemService.image(SessionUser.current(session), itemId, kind);
      return FileResponses.image(media.contentType(), media.bytes());
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return ResponseEntity.notFound().build();
    }
  }
}

package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ChangeHistoryView;
import com.ruzinn.dto.ItemImportResult;
import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.dto.ItemMasterView;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.MediaType;

public interface ItemMasterService {

  List<ItemMasterView> list(AuthSession actor, String query);

  ItemMasterView get(AuthSession actor, String itemId);

  ItemMasterForm form(AuthSession actor, String itemId);

  List<ChangeHistoryView> history(AuthSession actor, String itemId);

  ItemMasterView save(AuthSession actor, ItemMasterForm form);

  ItemMasterView save(AuthSession actor, ItemMasterForm form, String forcedAction);

  MediaResource image(AuthSession actor, String itemId, String kind);

  void delete(AuthSession actor, String itemId);

  byte[] template(AuthSession actor);

  ItemImportResult importExcel(AuthSession actor, InputStream input);

  record MediaResource(byte[] bytes, String contentType) {
    public String contentTypeOrJpeg() {
      return contentType == null || contentType.isBlank() ? MediaType.IMAGE_JPEG_VALUE : contentType;
    }
  }
}

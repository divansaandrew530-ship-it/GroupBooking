package com.ruzinn.service;

import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.dto.LoginHomeForm;
import com.ruzinn.dto.LoginHomeView;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.enums.AppLocale;
import java.util.List;
import java.util.Map;

public interface CmsTranslationService {

  void seedDefaults();

  void seedDefaults(LoginHomePage page);

  String ui(String key);

  String ui(String key, AppLocale locale);

  Map<String, String> uiMap(AppLocale locale);

  List<UiRow> uiRows(AppLocale locale);

  Map<String, List<UiRow>> uiGroups(AppLocale locale);

  void saveUi(AppLocale locale, Map<String, String> values);

  String text(String bundle, String entityId, String field, AppLocale locale, String fallback);

  LoginHomeView localize(LoginHomeView view, AppLocale locale);

  LoginHomeForm localizeForm(LoginHomeForm form, AppLocale locale);

  void saveLoginHome(LoginHomeForm form, AppLocale locale);

  void applyItemForm(ItemMasterForm form, AppLocale locale);

  void saveItemCopy(String itemId, ItemMasterForm form, AppLocale locale);

  String itemField(String itemId, String field, String fallback);

  void put(String bundle, String entityId, String field, AppLocale locale, String value);

  record UiRow(String key, String group, String english, String value) {}
}

package com.ruzinn.service;

import com.ruzinn.dto.LoginHomeForm;
import com.ruzinn.dto.LoginHomeView;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.entity.LoginHomeSlide;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface LoginHomeService {

  List<String> ICON_KEYS = List.of("flights", "hotels", "attractions", "support");

  LoginHomePage ensureDefaults();

  LoginHomeView published();

  LoginHomeForm editorForm();

  void save(LoginHomeForm form);

  void addSlide();

  void deleteSlide(Long slideId);

  LoginHomeSlide uploadSlideImage(Long slideId, MultipartFile file);

  MediaResource slideImage(Long slideId);

  record MediaResource(byte[] bytes, String contentType) {}
}

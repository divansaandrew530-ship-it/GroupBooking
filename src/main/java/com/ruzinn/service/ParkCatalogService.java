package com.ruzinn.service;

import com.ruzinn.dto.Experience;
import com.ruzinn.dto.ExperienceCatalog;
import java.util.List;
import java.util.Optional;

public interface ParkCatalogService {

  ExperienceCatalog getCatalog();

  List<Experience> experiences();

  List<Experience> extras();

  Experience require(String id);

  Optional<Experience> find(String id);
}

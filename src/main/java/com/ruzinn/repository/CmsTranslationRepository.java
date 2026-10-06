package com.ruzinn.repository;

import com.ruzinn.entity.CmsTranslation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CmsTranslationRepository extends JpaRepository<CmsTranslation, Long> {

  Optional<CmsTranslation> findByBundleAndEntityIdAndFieldNameAndLocale(
      String bundle, String entityId, String fieldName, String locale);

  List<CmsTranslation> findByBundleAndLocale(String bundle, String locale);

  List<CmsTranslation> findByBundleAndEntityIdAndLocale(String bundle, String entityId, String locale);
}

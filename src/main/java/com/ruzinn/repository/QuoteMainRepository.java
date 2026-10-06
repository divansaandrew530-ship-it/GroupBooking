package com.ruzinn.repository;

import com.ruzinn.entity.QuoteMain;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteMainRepository extends JpaRepository<QuoteMain, String> {

  List<QuoteMain> findByCompanyIdOrderByCreatedDateDesc(String companyId);

  List<QuoteMain> findAllByOrderByCreatedDateDesc();

  @Query(
      "select q from QuoteMain q where lower(q.userId) in :userIds order by q.createdDate desc")
  List<QuoteMain> findByNormalizedUserIdIn(@Param("userIds") Collection<String> userIds);
}

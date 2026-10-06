package com.ruzinn.repository;

import com.ruzinn.entity.ChangeHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {

  List<ChangeHistory> findByBookingRefNumOrderByChangedAtDescIdDesc(String bookingRefNum);

  List<ChangeHistory> findByEntityTypeAndEntityIdOrderByChangedAtDescIdDesc(String entityType, String entityId);

  List<ChangeHistory> findByEntityTypeOrderByChangedAtDescIdDesc(String entityType);
}

package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ChangeHistoryView;
import com.ruzinn.entity.ChangeHistory;
import com.ruzinn.repository.ChangeHistoryRepository;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.util.AuditActor;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeHistoryServiceImpl implements ChangeHistoryService {

  private static final Logger log = LoggerFactory.getLogger(ChangeHistoryServiceImpl.class);

  private static final int VALUE_LIMIT = 2000;

  private final ChangeHistoryRepository historyRepository;

  public ChangeHistoryServiceImpl(ChangeHistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  @Override
  @Transactional
  public void record(
      String entityType,
      String entityId,
      String bookingRefNum,
      String action,
      String fieldName,
      Object oldValue,
      Object newValue,
      String companyId,
      String companyName) {
    record(entityType, entityId, bookingRefNum, action, fieldName, oldValue, newValue, companyId, companyName, null);
  }

  @Override
  @Transactional
  public void record(
      String entityType,
      String entityId,
      String bookingRefNum,
      String action,
      String fieldName,
      Object oldValue,
      Object newValue,
      String companyId,
      String companyName,
      AuthSession actor) {
    String oldText = stringify(oldValue);
    String newText = stringify(newValue);
    if (USER_CREATE.equals(action)
        || PASSWORD_RESET.equals(action)
        || SLIDE_ADD.equals(action)
        || SLIDE_DELETE.equals(action)
        || ITEM_DELETE.equals(action)) {
      // always persist user/password/slide/delete events
    } else if (Objects.equals(oldText, newText)) {
      return;
    }
    AuditActor.Snapshot snapshot = actor != null ? AuditActor.from(actor) : AuditActor.current();
    ChangeHistory row = new ChangeHistory();
    row.setEntityType(blankToEmpty(entityType));
    row.setEntityId(blankToEmpty(entityId));
    row.setBookingRefNum(blankToNull(bookingRefNum));
    row.setAction(blankToEmpty(action));
    row.setFieldName(blankToNull(fieldName));
    row.setOldValue(oldText);
    row.setNewValue(newText);
    if (snapshot != null) {
      row.setActorUserId(blankToNull(snapshot.userId()));
      row.setActorUserName(blankToNull(snapshot.userName()));
      row.setActorEmail(blankToNull(snapshot.email()));
      row.setActorRole(blankToNull(snapshot.role()));
      if (companyId == null || companyId.isBlank()) {
        companyId = snapshot.companyId();
      }
      if (companyName == null || companyName.isBlank()) {
        companyName = snapshot.companyName();
      }
    }
    row.setCompanyId(blankToNull(companyId));
    row.setCompanyName(blankToNull(companyName));
    row.setChangedAt(Instant.now());
    historyRepository.save(row);
    log.debug(
        "History {} {} {}.{}: '{}' -> '{}' by {}",
        row.getAction(), row.getEntityType(), row.getEntityId(), row.getFieldName(),
        abbreviate(oldText), abbreviate(newText), row.getActorEmail());
  }

  /** Keeps debug lines short; full values are stored in the history table. */
  private static String abbreviate(String value) {
    if (value == null || value.length() <= 80) {
      return value;
    }
    return value.substring(0, 77) + "...";
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChangeHistoryView> listForBooking(String bookingRefNum) {
    if (bookingRefNum == null || bookingRefNum.isBlank()) {
      return List.of();
    }
    return historyRepository
        .findByBookingRefNumOrderByChangedAtDescIdDesc(bookingRefNum.trim().toUpperCase())
        .stream()
        .map(ChangeHistoryView::from)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChangeHistoryView> listForEntity(String entityType, String entityId) {
    if (entityType == null || entityType.isBlank() || entityId == null || entityId.isBlank()) {
      return List.of();
    }
    return historyRepository
        .findByEntityTypeAndEntityIdOrderByChangedAtDescIdDesc(entityType.trim(), entityId.trim())
        .stream()
        .map(ChangeHistoryView::from)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChangeHistoryView> listForEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      return List.of();
    }
    return historyRepository.findByEntityTypeOrderByChangedAtDescIdDesc(entityType.trim()).stream()
        .map(ChangeHistoryView::from)
        .toList();
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString().trim();
    if (text.length() > VALUE_LIMIT) {
      return text.substring(0, VALUE_LIMIT);
    }
    return text;
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

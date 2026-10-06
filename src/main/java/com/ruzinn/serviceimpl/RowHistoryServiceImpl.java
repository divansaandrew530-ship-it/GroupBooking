package com.ruzinn.serviceimpl;

import com.ruzinn.enums.EditableTable;
import com.ruzinn.repository.HistoryTableSchema;
import com.ruzinn.repository.RowHistoryRepository;
import com.ruzinn.repository.RowHistoryRepository.ArchiveInfo;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.AuditActor;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Keeps a copy of a row in {@code <table>_Hstry} before it is changed.
 *
 * <p>The history tables are created/updated once at startup ({@link #ensureHistoryTables()}); after
 * that {@link #archive} is a single {@code INSERT ... SELECT} on the caller's transaction.
 */
@Service
@Order(20)
public class RowHistoryServiceImpl implements RowHistoryService, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(RowHistoryServiceImpl.class);

  private final HistoryTableSchema schema;
  private final RowHistoryRepository repository;

  /** Columns copied on archive, per table. A table is missing here until its history table is ready. */
  private final Map<EditableTable, List<String>> copyColumns = new ConcurrentHashMap<>();

  public RowHistoryServiceImpl(HistoryTableSchema schema, RowHistoryRepository repository) {
    this.schema = schema;
    this.repository = repository;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureHistoryTables();
  }

  @Override
  public void ensureHistoryTables() {
    int ready = 0;
    for (EditableTable table : EditableTable.values()) {
      if (prepare(table) != null) {
        ready++;
      }
    }
    log.info("Audit history tables ready: {} of {}", ready, EditableTable.values().length);
  }

  @Override
  public void archive(EditableTable table, Object primaryKey, String action, String changedFields) {
    if (table == null || primaryKey == null || String.valueOf(primaryKey).isBlank()) {
      return;
    }
    List<String> columns = copyColumns.get(table);
    if (columns == null) {
      // Not prepared at startup (e.g. the source table was created later): prepare it now.
      log.warn("History table {} was not ready; preparing it now", table.historyTable());
      columns = prepare(table);
      if (columns == null) {
        log.warn("Skip archive of {} {}: no source table", table.sourceTable(), primaryKey);
        return;
      }
    }

    AuditActor.Snapshot actor = AuditActor.current();
    ArchiveInfo info =
        new ArchiveInfo(
            Timestamp.from(Instant.now()),
            actor == null ? null : actor.userId(),
            actor == null ? null : actor.userName(),
            actor == null ? null : actor.email(),
            action,
            changedFields);
    try {
      int rows = repository.insertSnapshot(table, columns, primaryKey, info);
      if (rows == 0) {
        log.warn("Archive {} {}: source row not found, nothing archived", table.sourceTable(), primaryKey);
      } else {
        log.debug(
            "Archived {} {} ({}, fields: {}) by {}",
            table.sourceTable(), primaryKey, action, changedFields, info.email());
      }
    } catch (RuntimeException ex) {
      log.error("Could not archive {} row {} ({})", table.sourceTable(), primaryKey, action, ex);
      throw new IllegalStateException("Could not archive " + table.sourceTable() + " row " + primaryKey, ex);
    }
  }

  @Override
  public int count(EditableTable table, Object primaryKey) {
    if (table == null || primaryKey == null || !copyColumns.containsKey(table)) {
      return 0;
    }
    try {
      return repository.count(table, primaryKey);
    } catch (RuntimeException ex) {
      log.error("Could not count {} rows for {}", table.historyTable(), primaryKey, ex);
      throw new IllegalStateException("Could not count " + table.historyTable() + " rows", ex);
    }
  }

  /** Creates/updates the history table and caches its copy columns; {@code null} when not possible. */
  private List<String> prepare(EditableTable table) {
    List<String> columns = schema.sync(table);
    if (columns.isEmpty()) {
      copyColumns.remove(table);
      return null;
    }
    copyColumns.put(table, List.copyOf(columns));
    return columns;
  }
}

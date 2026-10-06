package com.ruzinn.repository;

import com.ruzinn.enums.EditableTable;
import java.sql.Timestamp;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes {@code <table>_Hstry} rows. Runs on the caller's transaction (JdbcTemplate joins
 * the current JPA transaction), so an archive is rolled back together with the change it records.
 */
@Repository
public class RowHistoryRepository {

  private final JdbcTemplate jdbc;

  public RowHistoryRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  /** Who archived a row and why. Blank values are stored as NULL. */
  public record ArchiveInfo(
      Timestamp archivedAt, String userId, String userName, String email, String action, String changedFields) {}

  /**
   * Copies the current source row into the history table:
   *
   * <pre>
   * INSERT INTO company_Hstry (c1, c2, ..., archived_at, archived_by_user_id, ...)
   * SELECT c1, c2, ..., ?, ?, ... FROM company WHERE company_id = ?
   * </pre>
   *
   * @param columns source columns that exist in the history table
   * @return number of rows archived (0 when the source row does not exist)
   */
  public int insertSnapshot(EditableTable table, List<String> columns, Object primaryKey, ArchiveInfo info) {
    String columnList = String.join(", ", columns);
    String sql =
        "INSERT INTO " + table.historyTable() + " ("
            + columnList
            + ", archived_at, archived_by_user_id, archived_by_user_name, archived_by_email, archive_action, changed_fields)"
            + " SELECT " + columnList + ", ?, ?, ?, ?, ?, ?"
            + " FROM " + table.sourceTable()
            + " WHERE " + table.primaryKeyColumn() + " = ?";
    return jdbc.update(
        sql,
        info.archivedAt(),
        blankToNull(info.userId()),
        blankToNull(info.userName()),
        blankToNull(info.email()),
        blankToNull(info.action()),
        blankToNull(info.changedFields()),
        primaryKey);
  }

  /** How many archived versions a source row has. */
  public int count(EditableTable table, Object primaryKey) {
    String sql = "SELECT COUNT(*) FROM " + table.historyTable() + " WHERE " + table.primaryKeyColumn() + " = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, primaryKey);
    return count == null ? 0 : count;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}

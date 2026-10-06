package com.ruzinn.repository;

import com.ruzinn.enums.EditableTable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Creates and keeps the {@code <table>_Hstry} audit tables in step with their source tables.
 *
 * <p>Each history table has every source column, plus {@code hstry_id} (its own primary key) and the
 * archive columns ({@code archived_at}, {@code archived_by_*}, {@code archive_action},
 * {@code changed_fields}). The source primary key and unique indexes are removed from the history
 * table because one source row is archived many times.
 *
 * <p>All DDL runs on its own auto-commit connection, never inside a business transaction (MySQL
 * commits the open transaction on any DDL).
 */
@Component
public class HistoryTableSchema {

  private static final Logger log = LoggerFactory.getLogger(HistoryTableSchema.class);

  /** Columns that belong to the history table only; never copied from the source row. */
  public static final Set<String> ARCHIVE_COLUMNS =
      Set.of(
          "hstry_id",
          "archived_at",
          "archived_by_user_id",
          "archived_by_user_name",
          "archived_by_email",
          "archive_action",
          "changed_fields");

  // ---- information_schema queries (MySQL uses DATABASE(); H2 uses the PUBLIC schema) ------------

  private static final String CURRENT_SCHEMA = "(lower(table_schema) = 'public' OR table_schema = DATABASE())";

  private static final String SQL_TABLE_NAME =
      "SELECT table_name FROM information_schema.tables WHERE lower(table_name) = ? AND "
          + CURRENT_SCHEMA
          + " LIMIT 1";

  private static final String SQL_COLUMNS =
      "SELECT * FROM information_schema.columns WHERE lower(table_name) = ? AND "
          + CURRENT_SCHEMA
          + " ORDER BY ordinal_position";

  private static final String SQL_COLUMN_EXTRA =
      "SELECT extra FROM information_schema.columns WHERE lower(table_name) = ? AND lower(column_name) = ? AND "
          + CURRENT_SCHEMA
          + " LIMIT 1";

  private final DataSource dataSource;

  public HistoryTableSchema(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Creates or updates the history table for {@code table}.
   *
   * @return the source columns that can be copied into the history table, or an empty list when
   *     the source table does not exist (yet)
   */
  public List<String> sync(EditableTable table) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(true);
      boolean mysql = isMysql(connection);
      String source = table.sourceTable();
      String history = table.historyTable();

      if (tableName(connection, source) == null) {
        log.debug("Skip {}: source table {} does not exist", history, source);
        return List.of();
      }
      List<Column> sourceColumns = dataColumns(columns(connection, source, mysql));
      if (sourceColumns.isEmpty()) {
        return List.of();
      }
      if (tableName(connection, history) == null) {
        createHistoryTable(connection, history, sourceColumns, mysql);
      }

      removeSourceKeys(connection, history, mysql);

      Set<String> existing = columnNames(connection, history, mysql);
      addColumnIfMissing(connection, history, existing, "archived_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
      addColumnIfMissing(connection, history, existing, "archived_by_user_id", "VARCHAR(32)");
      addColumnIfMissing(connection, history, existing, "archived_by_user_name", "VARCHAR(255)");
      addColumnIfMissing(connection, history, existing, "archived_by_email", "VARCHAR(160)");
      addColumnIfMissing(connection, history, existing, "archive_action", "VARCHAR(40)");
      addColumnIfMissing(connection, history, existing, "changed_fields", "VARCHAR(500)");
      for (Column column : sourceColumns) {
        addColumnIfMissing(connection, history, existing, column.name(), column.definition());
      }

      ensureHistoryPrimaryKey(connection, history, existing, mysql);

      List<String> copy = new ArrayList<>();
      for (Column column : sourceColumns) {
        if (existing.contains(column.name().toLowerCase(Locale.ROOT))) {
          copy.add(column.name());
        }
      }
      return copy;
    } catch (SQLException ex) {
      throw new IllegalStateException("Could not create or update audit table " + table.historyTable(), ex);
    }
  }

  // ---- create / alter ---------------------------------------------------------------------------

  private void createHistoryTable(Connection connection, String history, List<Column> sourceColumns, boolean mysql)
      throws SQLException {
    StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(history).append(" (");
    ddl.append(hstryIdDefinition(mysql));
    for (Column column : sourceColumns) {
      ddl.append(", ").append(column.name()).append(' ').append(column.definition());
    }
    ddl.append(", archived_at TIMESTAMP NOT NULL")
        .append(", archived_by_user_id VARCHAR(32)")
        .append(", archived_by_user_name VARCHAR(255)")
        .append(", archived_by_email VARCHAR(160)")
        .append(", archive_action VARCHAR(40)")
        .append(", changed_fields VARCHAR(500)")
        .append(')');
    execute(connection, ddl.toString());
    log.info("Created audit table {}", history);
  }

  private void addColumnIfMissing(
      Connection connection, String table, Set<String> existing, String column, String definition)
      throws SQLException {
    String key = column.toLowerCase(Locale.ROOT);
    if (existing.contains(key)) {
      return;
    }
    execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    existing.add(key);
    log.info("Added column {}.{}", table, column);
  }

  /** {@code hstry_id} must exist and be the auto-generated primary key. */
  private void ensureHistoryPrimaryKey(Connection connection, String history, Set<String> existing, boolean mysql)
      throws SQLException {
    if (!existing.contains("hstry_id")) {
      execute(connection, "ALTER TABLE " + history + " ADD COLUMN " + hstryIdDefinition(mysql));
      existing.add("hstry_id");
      log.info("Added primary key {}.hstry_id", history);
    }
    if (mysql && !isAutoIncrement(connection, history, "hstry_id")) {
      executeFirstThatWorks(
          connection,
          "ALTER TABLE " + history + " MODIFY hstry_id BIGINT NOT NULL AUTO_INCREMENT",
          "ALTER TABLE " + history + " MODIFY hstry_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY");
      log.info("Made {}.hstry_id AUTO_INCREMENT", history);
    }
  }

  /**
   * A history table must not keep the source primary key or unique indexes: the same source row is
   * archived many times.
   */
  private void removeSourceKeys(Connection connection, String history, boolean mysql) throws SQLException {
    List<String> keyColumns = primaryKeyColumns(connection, history);
    boolean keyIsHstryId = keyColumns.size() == 1 && "hstry_id".equalsIgnoreCase(keyColumns.get(0));
    if (!keyColumns.isEmpty() && !keyIsHstryId) {
      if (mysql) {
        // MySQL refuses to drop the key while the column is still AUTO_INCREMENT.
        List<Column> columns = columns(connection, history, true);
        for (String keyColumn : keyColumns) {
          String type = definitionOf(columns, keyColumn);
          if (type != null) {
            tryExecute(connection, "ALTER TABLE " + history + " MODIFY " + keyColumn + " " + type + " NULL");
          }
        }
      }
      if (tryExecute(connection, "ALTER TABLE " + history + " DROP PRIMARY KEY")) {
        log.info("Dropped source primary key {} from {}", keyColumns, history);
      }
    }
    for (String index : uniqueIndexNames(connection, history)) {
      boolean dropped =
          tryExecute(connection, "ALTER TABLE " + history + " DROP INDEX " + index)
              || tryExecute(connection, "DROP INDEX IF EXISTS " + index)
              || tryExecute(connection, "ALTER TABLE " + history + " DROP CONSTRAINT " + index);
      if (dropped) {
        log.info("Dropped unique index {} from {}", index, history);
      } else {
        log.warn("Could not drop unique index {} from {}", index, history);
      }
    }
  }

  private static String hstryIdDefinition(boolean mysql) {
    return mysql
        ? "hstry_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY"
        : "hstry_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
  }

  // ---- metadata ---------------------------------------------------------------------------------

  /** The table name as stored in the database (case may differ), or {@code null} when missing. */
  private static String tableName(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SQL_TABLE_NAME)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    }
  }

  private static List<Column> columns(Connection connection, String table, boolean mysql) throws SQLException {
    List<Column> columns = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(SQL_COLUMNS)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("COLUMN_NAME");
          if (name != null) {
            columns.add(new Column(name, columnDefinition(rs, mysql)));
          }
        }
      }
    }
    return columns;
  }

  private static Set<String> columnNames(Connection connection, String table, boolean mysql) throws SQLException {
    Set<String> names = new LinkedHashSet<>();
    for (Column column : columns(connection, table, mysql)) {
      names.add(column.name().toLowerCase(Locale.ROOT));
    }
    return names;
  }

  private static List<Column> dataColumns(List<Column> columns) {
    return columns.stream()
        .filter(column -> !ARCHIVE_COLUMNS.contains(column.name().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private static String definitionOf(List<Column> columns, String name) {
    for (Column column : columns) {
      if (column.name().equalsIgnoreCase(name)) {
        return column.definition();
      }
    }
    return null;
  }

  private static boolean isAutoIncrement(Connection connection, String table, String column) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SQL_COLUMN_EXTRA)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      statement.setString(2, column.toLowerCase(Locale.ROOT));
      try (ResultSet rs = statement.executeQuery()) {
        String extra = rs.next() ? rs.getString(1) : null;
        return extra != null && extra.toLowerCase(Locale.ROOT).contains("auto_increment");
      }
    }
  }

  private static List<String> primaryKeyColumns(Connection connection, String table) throws SQLException {
    List<String> columns = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getPrimaryKeys(connection.getCatalog(), null, actualName(connection, table))) {
      while (rs.next()) {
        columns.add(rs.getString("COLUMN_NAME"));
      }
    }
    return columns;
  }

  private static List<String> uniqueIndexNames(Connection connection, String table) throws SQLException {
    Set<String> names = new LinkedHashSet<>();
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getIndexInfo(connection.getCatalog(), null, actualName(connection, table), true, false)) {
      while (rs.next()) {
        String name = rs.getString("INDEX_NAME");
        if (name == null || name.isBlank()) {
          continue;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("PRIMARY") || upper.startsWith("PRIMARY_KEY")) {
          continue;
        }
        names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private static String actualName(Connection connection, String table) throws SQLException {
    String actual = tableName(connection, table);
    return actual == null ? table : actual;
  }

  /** The column type to reuse in the history table (e.g. {@code varchar(255)}, {@code decimal(12,2)}). */
  private static String columnDefinition(ResultSet rs, boolean mysql) {
    if (mysql) {
      String columnType = stringOrNull(rs, "COLUMN_TYPE");
      if (columnType != null && !columnType.isBlank()) {
        return columnType;
      }
    }
    String typeName = stringOrNull(rs, "TYPE_NAME");
    if (typeName == null || typeName.isBlank()) {
      typeName = stringOrNull(rs, "DATA_TYPE");
    }
    if (typeName == null || typeName.isBlank()) {
      return "VARCHAR(255)";
    }
    typeName = typeName.trim();
    String upper = typeName.toUpperCase(Locale.ROOT);
    Integer length = intOrNull(rs, "CHARACTER_MAXIMUM_LENGTH");
    Integer precision = intOrNull(rs, "NUMERIC_PRECISION");
    Integer scale = intOrNull(rs, "NUMERIC_SCALE");

    if (upper.contains("CHAR") || upper.contains("BINARY") || upper.contains("BLOB")) {
      if (length != null && length > 0 && length < 1_000_000_000) {
        return typeName + "(" + length + ")";
      }
      return upper.contains("BLOB") || upper.contains("BINARY") ? "BLOB" : "VARCHAR(255)";
    }
    boolean decimal = upper.contains("DECIMAL") || upper.contains("NUMERIC") || upper.contains("NUMBER");
    if (decimal && precision != null && precision > 0) {
      return typeName + "(" + precision + "," + (scale == null ? 0 : scale) + ")";
    }
    return typeName;
  }

  private static boolean isMysql(Connection connection) throws SQLException {
    String product = connection.getMetaData().getDatabaseProductName();
    // Everything except H2 (tests / local) is treated as MySQL, as before.
    return product == null || !product.toUpperCase(Locale.ROOT).contains("H2");
  }

  // ---- execution helpers ------------------------------------------------------------------------

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /** Runs {@code sql}; returns {@code false} instead of failing (for dialect-specific statements). */
  private static boolean tryExecute(Connection connection, String sql) {
    try {
      execute(connection, sql);
      return true;
    } catch (SQLException ex) {
      log.debug("Statement not applied ({}): {}", ex.getMessage(), sql);
      return false;
    }
  }

  private static void executeFirstThatWorks(Connection connection, String... statements) throws SQLException {
    SQLException last = null;
    for (String sql : statements) {
      try {
        execute(connection, sql);
        return;
      } catch (SQLException ex) {
        last = ex;
      }
    }
    if (last != null) {
      throw last;
    }
  }

  private static String stringOrNull(ResultSet rs, String column) {
    try {
      return rs.getString(column);
    } catch (SQLException ex) {
      return null;
    }
  }

  private static Integer intOrNull(ResultSet rs, String column) {
    try {
      int value = rs.getInt(column);
      return rs.wasNull() ? null : value;
    } catch (SQLException ex) {
      return null;
    }
  }

  private record Column(String name, String definition) {}
}

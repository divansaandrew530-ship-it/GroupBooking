package com.ruzinn.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

@Component
public class CompanyIdNumericMigrator implements BeanPostProcessor, PriorityOrdered {

  private static final Logger log = LoggerFactory.getLogger(CompanyIdNumericMigrator.class);
  private static final AtomicBoolean RAN = new AtomicBoolean();

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DataSource dataSource && RAN.compareAndSet(false, true)) {
      migrate(dataSource);
    }
    return bean;
  }

  static void migrate(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(true);
      if (!tableExists(connection, "company")) {
        return;
      }
      String type = columnType(connection, "company", "company_id");
      if (type == null || isNumericType(type)) {
        return;
      }
      log.info("Converting company_id from {} to BIGINT for indexing", type);
      convertCompanyIds(connection);
      log.info("company_id columns now use BIGINT");
    } catch (SQLException ex) {
      throw new IllegalStateException("Could not convert company_id to a numeric column", ex);
    }
  }

  private static void convertCompanyIds(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try {
        statement.execute("SET FOREIGN_KEY_CHECKS=0");
      } catch (SQLException ignored) {
        // H2 and some MariaDB modes do not support this session flag
      }
    }
    populateIdMap(connection);
    addNumericColumn(connection, "company");
    addNumericColumn(connection, "app_user");
    if (tableExists(connection, "company_registration")) {
      addNumericColumn(connection, "company_registration");
    }
    fillNumericColumn(connection, "company");
    fillNumericColumn(connection, "app_user");
    if (tableExists(connection, "company_registration")) {
      fillNumericColumn(connection, "company_registration");
    }
    rewriteIdColumn(connection, "company", true);
    rewriteIdColumn(connection, "app_user", false);
    if (tableExists(connection, "company_registration")) {
      rewriteIdColumn(connection, "company_registration", false);
    }
    rewriteBookingCompanyIds(connection, "booking_main");
    rewriteBookingCompanyIds(connection, "booking_details");

    try (Statement statement = connection.createStatement();
        ResultSet max =
            statement.executeQuery("SELECT COALESCE(MAX(company_id), 0) + 1 FROM company")) {
      max.next();
      long next = max.getLong(1);
      try {
        statement.execute("ALTER TABLE company AUTO_INCREMENT = " + next);
      } catch (SQLException ignored) {
        statement.execute("ALTER TABLE company ALTER COLUMN company_id RESTART WITH " + next);
      }
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS company_id_map");
      try {
        statement.execute("SET FOREIGN_KEY_CHECKS=1");
      } catch (SQLException ignored) {
        // session flag is optional
      }
    }
  }

  private static void populateIdMap(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS company_id_map");
      statement.execute(
          "CREATE TABLE company_id_map (old_id VARCHAR(80) NOT NULL PRIMARY KEY, new_id BIGINT NOT NULL UNIQUE)");
    }
    List<String> oldIds = new ArrayList<>();
    String superAdminOldId = null;
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT company_id FROM company ORDER BY created_date ASC, company_id ASC")) {
      while (rows.next()) {
        String oldId = rows.getString(1);
        if (oldId == null || oldId.isBlank()) {
          continue;
        }
        oldIds.add(oldId);
        if (superAdminOldId == null && isSuperAdminOldId(oldId)) {
          superAdminOldId = oldId;
        }
      }
    }
    long nextId = 1L;
    try (PreparedStatement insert =
        connection.prepareStatement("INSERT INTO company_id_map (old_id, new_id) VALUES (?, ?)")) {
      if (superAdminOldId != null) {
        insert.setString(1, superAdminOldId);
        insert.setLong(2, 1L);
        insert.executeUpdate();
        nextId = 2L;
      }
      for (String oldId : oldIds) {
        if (oldId.equals(superAdminOldId)) {
          continue;
        }
        insert.setString(1, oldId);
        insert.setLong(2, nextId++);
        insert.executeUpdate();
      }
    }
  }

  private static boolean isSuperAdminOldId(String oldId) {
    return "WGT".equalsIgnoreCase(oldId) || "1".equals(oldId);
  }

  private static void addNumericColumn(Connection connection, String table) throws SQLException {
    if (columnExists(connection, table, "company_id_num")) {
      return;
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE " + table + " ADD COLUMN company_id_num BIGINT NULL");
    }
  }

  private static void fillNumericColumn(Connection connection, String table) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "UPDATE "
              + table
              + " t INNER JOIN company_id_map m ON t.company_id = m.old_id SET t.company_id_num = m.new_id");
      if (!"company".equals(table)) {
        statement.execute(
            "UPDATE " + table + " SET company_id_num = 1 WHERE company_id_num IS NULL");
      }
    }
  }

  private static void rewriteIdColumn(Connection connection, String table, boolean primaryKey)
      throws SQLException {
    if ("app_user".equals(table)) {
      dropIndexIfPresent(connection, table, "idx_app_user_company");
    } else if ("company_registration".equals(table)) {
      dropIndexIfPresent(connection, table, "idx_company_registration_company");
    }
    try (Statement statement = connection.createStatement()) {
      if (primaryKey) {
        try {
          statement.execute("ALTER TABLE " + table + " DROP PRIMARY KEY");
        } catch (SQLException ignored) {
          // column drop also removes the primary key on some servers
        }
      }
      statement.execute("ALTER TABLE " + table + " DROP COLUMN company_id");
      statement.execute("ALTER TABLE " + table + " CHANGE company_id_num company_id BIGINT NOT NULL");
      if (primaryKey) {
        statement.execute("ALTER TABLE " + table + " ADD PRIMARY KEY (company_id)");
        statement.execute("ALTER TABLE " + table + " MODIFY company_id BIGINT NOT NULL AUTO_INCREMENT");
      } else if ("app_user".equals(table)) {
        statement.execute("CREATE INDEX idx_app_user_company ON app_user (company_id)");
      } else if ("company_registration".equals(table)) {
        statement.execute(
            "CREATE INDEX idx_company_registration_company ON company_registration (company_id)");
      }
    }
  }

  private static void rewriteBookingCompanyIds(Connection connection, String table) throws SQLException {
    if (!tableExists(connection, table) || !columnExists(connection, table, "company_id")) {
      return;
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "UPDATE "
              + table
              + " t INNER JOIN company_id_map m ON t.company_id = m.old_id SET t.company_id = CONCAT(m.new_id)");
    }
  }

  private static void dropIndexIfPresent(Connection connection, String table, String index)
      throws SQLException {
    if (!indexExists(connection, table, index)) {
      return;
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE " + table + " DROP INDEX " + index);
    }
  }

  private static boolean tableExists(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT 1
            FROM information_schema.tables
            WHERE lower(table_name) = ?
              AND (DATABASE() IS NULL OR table_schema = DATABASE())
            LIMIT 1
            """)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static boolean columnExists(Connection connection, String table, String column)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT 1
            FROM information_schema.columns
            WHERE lower(table_name) = ? AND lower(column_name) = ?
              AND (DATABASE() IS NULL OR table_schema = DATABASE())
            LIMIT 1
            """)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      statement.setString(2, column.toLowerCase(Locale.ROOT));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static String columnType(Connection connection, String table, String column)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT data_type
            FROM information_schema.columns
            WHERE lower(table_name) = ? AND lower(column_name) = ?
              AND (DATABASE() IS NULL OR table_schema = DATABASE())
            LIMIT 1
            """)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      statement.setString(2, column.toLowerCase(Locale.ROOT));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  private static boolean indexExists(Connection connection, String table, String index)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT 1
            FROM information_schema.statistics
            WHERE lower(table_name) = ? AND lower(index_name) = ?
              AND (DATABASE() IS NULL OR table_schema = DATABASE())
            LIMIT 1
            """)) {
      statement.setString(1, table.toLowerCase(Locale.ROOT));
      statement.setString(2, index.toLowerCase(Locale.ROOT));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static boolean isNumericType(String type) {
    String normalized = type.toLowerCase(Locale.ROOT);
    return normalized.contains("int")
        || normalized.contains("decimal")
        || normalized.contains("numeric")
        || normalized.contains("number");
  }
}

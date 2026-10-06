package com.ruzinn.audit;

import com.ruzinn.enums.EditableTable;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RowHistoryTableTest {

  @Autowired private javax.sql.DataSource dataSource;

  @Test
  void clonesEditableTablesWithHstrySuffixAndArchiveColumns() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    for (EditableTable table : EditableTable.values()) {
      org.assertj.core.api.Assertions.assertThat(tableExists(jdbc, table.sourceTable()))
          .as(table.sourceTable())
          .isTrue();
      org.assertj.core.api.Assertions.assertThat(tableExists(jdbc, table.historyTable()))
          .as(table.historyTable())
          .isTrue();
      List<String> source = tableColumns(jdbc, table.sourceTable());
      List<String> history = tableColumns(jdbc, table.historyTable());
      org.assertj.core.api.Assertions.assertThat(history)
          .as(table.historyTable() + " source columns")
          .containsAll(source);
      org.assertj.core.api.Assertions.assertThat(history)
          .as(table.historyTable() + " archive columns")
          .contains(
              "hstry_id",
              "archived_at",
              "archived_by_user_id",
              "archived_by_user_name",
              "archived_by_email",
              "archive_action",
              "changed_fields");
    }
  }

  private static boolean tableExists(JdbcTemplate jdbc, String table) {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE lower(table_name) = ?
            """,
            Integer.class,
            table.toLowerCase(Locale.ROOT));
    return count != null && count > 0;
  }

  private static List<String> tableColumns(JdbcTemplate jdbc, String table) {
    return jdbc
        .queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE lower(table_name) = ?
            ORDER BY ordinal_position
            """,
            String.class,
            table.toLowerCase(Locale.ROOT))
        .stream()
        .map(name -> name.toLowerCase(Locale.ROOT))
        .toList();
  }
}

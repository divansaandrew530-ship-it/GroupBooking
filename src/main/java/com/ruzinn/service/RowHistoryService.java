package com.ruzinn.service;

import com.ruzinn.enums.EditableTable;

public interface RowHistoryService {

  void ensureHistoryTables();

  void archive(EditableTable table, Object primaryKey, String action, String changedFields);

  int count(EditableTable table, Object primaryKey);
}

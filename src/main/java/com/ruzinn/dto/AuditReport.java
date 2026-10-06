package com.ruzinn.dto;

import java.util.List;

public record AuditReport(
    String title,
    String entityType,
    String entityId,
    String createdAt,
    String createdBy,
    List<AuditEditView> edits,
    int archivedRowCount) {}

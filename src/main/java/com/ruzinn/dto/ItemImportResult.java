package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public record ItemImportResult(int created, int updated, int skipped, List<String> errors) {

  public static ItemImportResult empty() {
    return new ItemImportResult(0, 0, 0, List.of());
  }

  public ItemImportResult withError(String error) {
    List<String> next = new ArrayList<>(errors);
    next.add(error);
    return new ItemImportResult(created, updated, skipped, List.copyOf(next));
  }

  public String summary() {
    return created
        + " created, "
        + updated
        + " updated, "
        + skipped
        + " skipped"
        + (errors.isEmpty() ? "." : ". " + errors.size() + " row error(s).");
  }
}

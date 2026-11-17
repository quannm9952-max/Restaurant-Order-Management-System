package com.restaurant.dto.preparation;

import java.util.List;
import java.util.Map;

public record PreparationQueue(List<PreparationOrder> orders,
    Map<String, Long> counts, List<TableOption> tables, int lineCount,
    int tableCount, String revision) {
  public record TableOption(Integer id, String name) {
  }

  public record Update(String revision, long readyLines) {
  }
}

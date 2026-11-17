package com.restaurant.repository;

import com.restaurant.dto.report.ReportModels.DateRange;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class ReportParameters {
  private ReportParameters() {
  }

  static MapSqlParameterSource range(DateRange range) {
    return new MapSqlParameterSource("from", range.start()).addValue("until",
        range.endExclusive());
  }

  static String literalSearch(String value) {
    return com.restaurant.repository.support.SqlSearch.containsLiteral(value);
  }
}

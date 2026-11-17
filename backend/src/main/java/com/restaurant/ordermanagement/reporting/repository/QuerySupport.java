package com.restaurant.ordermanagement.reporting.repository;

import com.restaurant.ordermanagement.reporting.dto.ReportingModels.DateRange;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class QuerySupport {
  private QuerySupport() {
  }

  static MapSqlParameterSource range(DateRange range) {
    return new MapSqlParameterSource("from", range.start()).addValue("until",
        range.endExclusive());
  }

  static String literalSearch(String value) {
    return com.restaurant.repository.support.SqlSearch.containsLiteral(value);
  }
}

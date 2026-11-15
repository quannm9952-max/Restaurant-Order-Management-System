package com.restaurant.ordermanagement.reporting.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import com.restaurant.ordermanagement.reporting.dto.ReportingModels.DateRange;

final class QuerySupport {
    private QuerySupport() {}
    static MapSqlParameterSource range(DateRange range) {
        return new MapSqlParameterSource("from", range.start()).addValue("until", range.endExclusive());
    }
    static String literalSearch(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}

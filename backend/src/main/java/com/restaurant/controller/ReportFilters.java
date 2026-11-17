package com.restaurant.controller;

import com.restaurant.dto.report.ReportModels.DateRange;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportFilters {
  private final Clock clock;
  private final String defaultEnd;

  public ReportFilters(Clock clock,
      @Value("${reporting.default-end-date:}") String defaultEnd) {
    this.clock = clock;
    this.defaultEnd = defaultEnd;
  }

  public DateRange range(String from, String to) {
    try {
      LocalDate end = to == null || to.isBlank()
          ? (defaultEnd.isBlank()
              ? LocalDate.now(clock)
              : LocalDate.parse(defaultEnd))
          : LocalDate.parse(to);
      LocalDate start = from == null || from.isBlank()
          ? end.minusDays(6)
          : LocalDate.parse(from);
      return new DateRange(start, end);
    } catch (java.time.DateTimeException exception) {
      throw new IllegalArgumentException(
          "Choose valid dates in YYYY-MM-DD format.");
    }
  }

  public <E extends Enum<E>> E status(String value, Class<E> type) {
    if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value))
      return null;
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Choose a valid filter value.");
    }
  }
}

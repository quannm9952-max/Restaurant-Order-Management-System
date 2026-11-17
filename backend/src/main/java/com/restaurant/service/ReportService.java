package com.restaurant.service;

import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.repository.PaymentReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MANAGER')")
public class ReportService {
  private final PaymentReportRepository payments;

  public ReportService(PaymentReportRepository payments) {
    this.payments = payments;
  }

  // Summary, chart and recent rows must see the same snapshot during concurrent
  // payments.
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public RevenueReport report(DateRange range, Grouping grouping) {
    RevenueSummary summary = payments.revenueSummary(range);
    List<DailyRevenue> daily = payments.dailyRevenue(range);
    Map<LocalDate, DailyRevenue> grouped = new TreeMap<>();
    for (DailyRevenue day : daily) {
      LocalDate key = grouping == Grouping.WEEK
          ? day.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
          : day.date();
      grouped.merge(key,
          new DailyRevenue(key, day.transactions(), day.amount()),
          (left, right) -> new DailyRevenue(key,
              left.transactions() + right.transactions(),
              left.amount().add(right.amount())));
    }
    BigDecimal max = grouped.values().stream().map(DailyRevenue::amount)
        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    BigDecimal ceiling = chartCeiling(max);
    List<ChartPoint> points = new ArrayList<>();
    for (DailyRevenue value : grouped.values()) {
      BigDecimal height = max.signum() == 0
          ? BigDecimal.ZERO
          : value.amount().multiply(BigDecimal.valueOf(100)).divide(ceiling, 2,
              RoundingMode.HALF_UP);
      points.add(new ChartPoint(value.date(),
          grouping == Grouping.WEEK ? value.date().plusDays(6) : value.date(),
          value.transactions(), value.amount(), height));
    }
    return new RevenueReport(range, grouping, summary, points,
        payments.recentSuccessful(range), ceiling);
  }

  private BigDecimal chartCeiling(BigDecimal maximum) {
    if (maximum.signum() == 0)
      return BigDecimal.valueOf(400);
    BigDecimal targetStep = maximum.divide(BigDecimal.valueOf(4));
    BigDecimal magnitude = BigDecimal.ONE
        .scaleByPowerOfTen(targetStep.precision() - targetStep.scale() - 1);
    for (int factor : new int[]{1, 2, 3, 5, 10}) {
      BigDecimal step = magnitude.multiply(BigDecimal.valueOf(factor));
      if (step.compareTo(targetStep) >= 0)
        return step.multiply(BigDecimal.valueOf(4));
    }
    throw new IllegalStateException("Unable to determine chart scale");
  }
}

package com.restaurant.dto.report;

import com.restaurant.entity.enums.PaymentStatus;
import com.restaurant.entity.enums.PaymentMethod;

import com.restaurant.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Read models only: no duplicate JPA entities or reporting tables. */
public final class ReportModels {
  private ReportModels() {
  }

  public enum Grouping {
    AUTO, DAY, WEEK, MONTH;

    public Grouping resolve(DateRange range) {
      if (this != AUTO)
        return this;
      return range.days() <= 31 ? DAY : range.days() <= 90 ? WEEK : MONTH;
    }
  }

  public static String label(Enum<?> value) {
    String text = value.name().toLowerCase(Locale.ROOT);
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  public record DateRange(LocalDate from, LocalDate to) {
    public DateRange {
      if (from == null || to == null || from.isAfter(to)
          || to.equals(LocalDate.MAX)) {
        throw new IllegalArgumentException(
            "Choose a valid start date and end date.");
      }
    }

    public LocalDateTime start() {
      return from.atStartOfDay();
    }

    public long days() {
      return ChronoUnit.DAYS.between(from, to) + 1;
    }

    public LocalDateTime endExclusive() {
      return to.plusDays(1).atStartOfDay();
    }

    public String display() {
      return from.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
          + " – " + to.format(
              DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }
  }

  public record Page<T>(List<T> content, long total, int number, int size) {
    public Page {
      content = List.copyOf(content);
    }

    public long pages() {
      return Math.max(1L, (total + size - 1) / size);
    }

    public boolean hasPrevious() {
      return number > 0;
    }

    public boolean hasNext() {
      return number + 1L < pages();
    }

    public long first() {
      return content.isEmpty() ? 0 : (long) number * size + 1;
    }

    public long last() {
      return content.isEmpty() ? 0 : first() + content.size() - 1;
    }
  }

  public record OrderRow(int orderId, String tableName, String employeeName,
      LocalDateTime createdAt, OrderStatus status, BigDecimal totalAmount,
      String notes) {
    public String code() {
      return "ORD-" + orderId;
    }

    public String statusLabel() {
      return label(status);
    }
  }

  public record OrderLine(int detailId, String itemName, int quantity,
      BigDecimal unitPrice, String preparationStatus) {
    public BigDecimal lineTotal() {
      return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean cancelled() {
      return "CANCELLED".equals(preparationStatus);
    }

    public String statusLabel() {
      String value = preparationStatus.toLowerCase(Locale.ROOT);
      return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
  }

  public record OrderDetails(OrderRow order, List<OrderLine> items,
      List<PaymentRow> payments) {
    public OrderDetails {
      items = List.copyOf(items);
      payments = List.copyOf(payments);
    }

    public BigDecimal calculatedTotal() {
      return items.stream().filter(item -> !item.cancelled())
          .map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }

  public record PaymentRow(int paymentId, int orderId,
      LocalDateTime paymentTime, BigDecimal amount, String paymentMethod,
      PaymentStatus status, String transactionReference, String tableName,
      OrderStatus orderStatus) {
    public String code() {
      return "PAY-" + paymentId;
    }

    public String orderCode() {
      return "ORD-" + orderId;
    }

    public String statusLabel() {
      return label(status);
    }

    public String methodLabel() {
      return PaymentMethod.display(paymentMethod);
    }

    public boolean includedInRevenue() {
      return status == PaymentStatus.SUCCESSFUL
          && orderStatus == OrderStatus.COMPLETED;
    }
  }

  public record PaymentCounts(long successful, long pending, long failed) {
    public long total() {
      return successful + pending + failed;
    }
  }

  public record RevenueSummary(BigDecimal totalRevenue, long successful,
      long pending, long failed, long inconsistentSuccessful) {
    public long excluded() {
      return pending + failed + inconsistentSuccessful;
    }
  }

  public record MethodRevenue(PaymentMethod method, long transactions,
      BigDecimal revenue) {
    public String label() {
      return method.label();
    }
  }

  public record DailyRevenue(LocalDate date, long transactions,
      BigDecimal amount) {
  }

  public record ChartPoint(LocalDate start, LocalDate end, long transactions,
      BigDecimal amount, BigDecimal heightPercent) {
    public String display() {
      DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM",
          Locale.ENGLISH);
      return start.equals(end)
          ? start.format(format)
          : start.format(format) + " – " + end.format(format);
    }
  }

  public record RevenueReport(DateRange range, Grouping grouping,
      RevenueSummary summary, List<ChartPoint> points,
      List<PaymentRow> recentPayments, BigDecimal chartCeiling,
      List<MethodRevenue> methods) {
    public RevenueReport {
      points = List.copyOf(points);
      recentPayments = List.copyOf(recentPayments);
      methods = List.copyOf(methods);
    }

    public BigDecimal cashRevenue() {
      return methodRevenue(PaymentMethod.CASH);
    }

    public BigDecimal bankTransferRevenue() {
      return methodRevenue(PaymentMethod.BANK_TRANSFER);
    }

    private BigDecimal methodRevenue(PaymentMethod method) {
      return methods.stream().filter(row -> row.method() == method)
          .map(MethodRevenue::revenue).findFirst().orElse(BigDecimal.ZERO);
    }

    public List<BigDecimal> axisTicks() {
      BigDecimal step = chartCeiling.divide(BigDecimal.valueOf(4));
      return List.of(chartCeiling, step.multiply(BigDecimal.valueOf(3)),
          step.multiply(BigDecimal.valueOf(2)), step, BigDecimal.ZERO);
    }

    public String chartTitle() {
      return switch (grouping) {
        case MONTH -> "Monthly revenue · USD";
        case WEEK -> "Weekly revenue · USD";
        default -> "Daily revenue · USD";
      };
    }

    public String periodLabel(ChartPoint point) {
      return grouping == Grouping.MONTH
          ? point.start()
              .format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
          : point.display();
    }

    public String axisLabel(ChartPoint point) {
      return grouping == Grouping.MONTH
          ? periodLabel(point)
          : point.start()
              .format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH));
    }

    public String coveredDates(ChartPoint point) {
      LocalDate start = point.start().isBefore(range.from())
          ? range.from()
          : point.start();
      LocalDate end = point.end().isAfter(range.to())
          ? range.to()
          : point.end();
      var format = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
      return start.equals(end)
          ? start.format(format)
          : start.format(format) + " – " + end.format(format);
    }

    public boolean dense() {
      return points.size() > 7;
    }

    public boolean showAxisLabel(int index) {
      int stride = Math.max(1, (points.size() + 4) / 6);
      if (!dense())
        return true;
      int last = points.size() - 1;
      return index == 0 || index == last
          || (index % stride == 0 && index <= last - stride);
    }

    public int minimumChartWidth() {
      if (dense())
        return Math.max(440, points.size() * 16 + 80);
      int slot = grouping == Grouping.MONTH ? 72 : 50;
      return Math.max(240, points.size() * slot + 80);
    }
  }
}

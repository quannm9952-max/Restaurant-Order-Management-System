package com.restaurant.ordermanagement.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Read models only: no duplicate JPA entities or reporting tables. */
public final class ReportingModels {
  private ReportingModels() {
  }

  public enum OrderStatus {
    OPEN, COMPLETED, CANCELLED
  }

  public enum PaymentStatus {
    PENDING, SUCCESSFUL, FAILED
  }

  public enum Grouping {
    DAY, WEEK
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
      List<PaymentRow> recentPayments, BigDecimal chartCeiling) {
    public RevenueReport {
      points = List.copyOf(points);
      recentPayments = List.copyOf(recentPayments);
    }

    public List<BigDecimal> axisTicks() {
      BigDecimal step = chartCeiling.divide(BigDecimal.valueOf(4));
      return List.of(chartCeiling, step.multiply(BigDecimal.valueOf(3)),
          step.multiply(BigDecimal.valueOf(2)), step, BigDecimal.ZERO);
    }
  }
}

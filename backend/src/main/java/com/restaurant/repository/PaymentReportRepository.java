package com.restaurant.repository;

import com.restaurant.entity.enums.PaymentStatus;

import com.restaurant.entity.enums.OrderStatus;

import com.restaurant.dto.report.ReportModels.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Aggregates directly from Payment. Joining order lines here would multiply
 * payments.
 */
@Repository
public class PaymentReportRepository {
  private static final String FROM = " FROM Payment p JOIN Orders "
      + "o ON o.order_id = p.order_id "
      + " JOIN Restaurant_tables t ON t.table_id = o.table_id ";
  private static final String COLUMNS = "SELECT p.payment_id, p.order_id, "
      + "p.payment_time, p.amount, p.payment_method, p.status,"
      + " p.transaction_reference, t.table_name, o.status AS order_status ";
  private static final String RANGE = " WHERE p.payment_time >= "
      + ":from AND p.payment_time < :until ";
  private static final String SEARCH = " AND (CONCAT('PAY-', "
      + "p.payment_id) LIKE :query ESCAPE '!' "
      + " OR CONCAT('ORD-', p.order_id) LIKE :query ESCAPE '!') ";
  private static final String INCLUDED = "p.status = 'SUCCESSFUL' "
      + "AND o.status = 'COMPLETED'";
  private final NamedParameterJdbcTemplate jdbc;

  public PaymentReportRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Page<PaymentRow> findPayments(DateRange range, String query,
      PaymentStatus status, int page, int size) {
    var parameters = ReportParameters.range(range)
        .addValue("query", ReportParameters.literalSearch(query))
        .addValue("status", status == null ? null : status.name(),
            java.sql.Types.VARCHAR)
        .addValue("limit", size).addValue("offset", (long) page * size);
    String filter = RANGE + SEARCH
        + " AND (:status IS NULL OR p.status = :status) ";
    long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + filter,
        parameters, Long.class);
    List<PaymentRow> rows = jdbc.query(
        COLUMNS + FROM + filter + " ORDER BY p.payment_time DESC, "
            + "p.payment_id DESC LIMIT :limit OFFSET :offset",
        parameters, this::map);
    return new Page<>(rows, total, page, size);
  }

  public PaymentCounts counts(DateRange range, String query) {
    return jdbc.queryForObject("""
        SELECT COUNT(CASE WHEN p.status = 'SUCCESSFUL' THEN 1 END) AS
        successful,
               COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END) AS pending,
               COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) AS failed
        """ + FROM + RANGE + SEARCH,
        ReportParameters.range(range).addValue("query",
            ReportParameters.literalSearch(query)),
        (rs, row) -> new PaymentCounts(rs.getLong("successful"),
            rs.getLong("pending"), rs.getLong("failed")));
  }

  public Optional<PaymentRow> findById(int id) {
    return jdbc
        .query(COLUMNS + FROM + " WHERE p.payment_id = :id",
            new MapSqlParameterSource("id", id), this::map)
        .stream().findFirst();
  }

  public List<PaymentRow> findByOrder(int orderId) {
    return jdbc.query(
        COLUMNS + FROM + " WHERE p.order_id = :id ORDER BY "
            + "p.payment_time DESC, p.payment_id DESC",
        new MapSqlParameterSource("id", orderId), this::map);
  }

  public RevenueSummary revenueSummary(DateRange range) {
    return jdbc.queryForObject("""
        SELECT COALESCE(SUM(CASE WHEN %s THEN p.amount ELSE 0 END), 0) AS
        revenue,
               COUNT(CASE WHEN %s THEN 1 END) AS successful,
               COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END) AS pending,
               COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) AS failed,
               COUNT(CASE WHEN p.status = 'SUCCESSFUL' AND o.status <>
               'COMPLETED' THEN 1 END) AS inconsistent
        """.formatted(INCLUDED, INCLUDED) + FROM + RANGE,
        ReportParameters.range(range),
        (rs, row) -> new RevenueSummary(rs.getBigDecimal("revenue"),
            rs.getLong("successful"), rs.getLong("pending"),
            rs.getLong("failed"), rs.getLong("inconsistent")));
  }

  public List<DailyRevenue> dailyRevenue(DateRange range) {
    return jdbc.query(
        "SELECT CAST(p.payment_time AS DATE) AS "
            + "report_date, COUNT(*) AS transactions, "
            + "SUM(p.amount) AS revenue " + FROM + RANGE + " AND " + INCLUDED
            + " GROUP BY CAST(p.payment_time AS DATE) ORDER BY report_date",
        ReportParameters.range(range),
        (rs, row) -> new DailyRevenue(rs.getDate("report_date").toLocalDate(),
            rs.getLong("transactions"), rs.getBigDecimal("revenue")));
  }

  public List<PaymentRow> recentSuccessful(DateRange range) {
    return jdbc.query(
        COLUMNS + FROM + RANGE + " AND " + INCLUDED
            + " ORDER BY p.payment_time DESC, p.payment_id DESC LIMIT 3",
        ReportParameters.range(range), this::map);
  }

  private PaymentRow map(ResultSet rs, int row) throws SQLException {
    return new PaymentRow(rs.getInt("payment_id"), rs.getInt("order_id"),
        rs.getTimestamp("payment_time").toLocalDateTime(),
        rs.getBigDecimal("amount"), rs.getString("payment_method"),
        PaymentStatus.valueOf(rs.getString("status")),
        rs.getString("transaction_reference"), rs.getString("table_name"),
        OrderStatus.valueOf(rs.getString("order_status")));
  }
}

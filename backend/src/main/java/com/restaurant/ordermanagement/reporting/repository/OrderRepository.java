package com.restaurant.ordermanagement.reporting.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.restaurant.ordermanagement.reporting.dto.ReportingModels.*;

/** Read access to the existing TV2 tables; deliberately has no save/update operations. */
@Repository
public class OrderRepository {
    private static final String FROM = " FROM Orders o JOIN Restaurant_tables t ON t.table_id = o.table_id "
            + " JOIN Employee e ON e.employee_id = o.employee_id ";
    private static final String COLUMNS = "SELECT o.order_id, t.table_name, e.full_name, o.created_at, "
            + "o.status, o.total_amount, o.notes ";
    private static final String FILTER = " WHERE (:status IS NULL OR o.status = :status) "
            + " AND (CONCAT('ORD-', o.order_id) LIKE :query ESCAPE '!' "
            + " OR t.table_name LIKE :query ESCAPE '!') ";
    private final NamedParameterJdbcTemplate jdbc;

    public OrderRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Page<OrderRow> findOrders(String query, OrderStatus status, int page, int size) {
        var parameters = new MapSqlParameterSource("query", QuerySupport.literalSearch(query))
                .addValue("status", status == null ? null : status.name(), java.sql.Types.VARCHAR)
                .addValue("limit", size).addValue("offset", (long) page * size);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + FILTER, parameters, Long.class);
        List<OrderRow> rows = jdbc.query(COLUMNS + FROM + FILTER
                + " ORDER BY o.created_at DESC, o.order_id DESC LIMIT :limit OFFSET :offset", parameters, this::map);
        return new Page<>(rows, total, page, size);
    }

    public Optional<OrderRow> findById(int id) {
        return jdbc.query(COLUMNS + FROM + " WHERE o.order_id = :id",
                new MapSqlParameterSource("id", id), this::map).stream().findFirst();
    }

    public List<OrderLine> findDetails(int orderId) {
        return jdbc.query("""
                SELECT order_detail_id, item_name_at_order, quantity, unit_price_at_order, preparation_status
                FROM Order_Detail WHERE order_id = :id ORDER BY order_detail_id
                """, new MapSqlParameterSource("id", orderId), (rs, row) -> new OrderLine(
                rs.getInt("order_detail_id"), rs.getString("item_name_at_order"), rs.getInt("quantity"),
                rs.getBigDecimal("unit_price_at_order"), rs.getString("preparation_status")));
    }

    private OrderRow map(ResultSet rs, int row) throws SQLException {
        return new OrderRow(rs.getInt("order_id"), rs.getString("table_name"), rs.getString("full_name"),
                rs.getTimestamp("created_at").toLocalDateTime(), OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("total_amount"), rs.getString("notes"));
    }
}

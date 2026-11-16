package com.restaurant.checkout.repository;

import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.context.annotation.Primary
public class CheckoutRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public CheckoutRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> rows(String sql, Map<String, ?> params) {
    return jdbc.query(
        sql,
        params,
        (rs, n) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            Object v = rs.getObject(i);
            if (v instanceof Timestamp t) v = t.toLocalDateTime().toString().replace('T', ' ');
            row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), v);
          }
          return row;
        });
  }

  public Map<String, Object> one(String sql, Map<String, ?> params) {
    var rows = rows(sql, params);
    if (rows.isEmpty()) throw new IllegalArgumentException("Record not found.");
    return rows.getFirst();
  }

  public long count(String sql, Map<String, ?> params) {
    return jdbc.queryForObject(sql, params, Long.class);
  }

  public int update(String sql, Map<String, ?> params) {
    return jdbc.update(sql, params);
  }
}

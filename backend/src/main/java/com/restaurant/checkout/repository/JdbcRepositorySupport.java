package com.restaurant.checkout.repository;

import com.restaurant.exceptions.NotFoundException;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class JdbcRepositorySupport {
  protected final NamedParameterJdbcTemplate jdbc;

  protected JdbcRepositorySupport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  protected List<Map<String, Object>> rows(String sql, Map<String, ?> params) {
    return jdbc.query(sql, params, (rs, n) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
        Object v = rs.getObject(i);
        if (v instanceof Timestamp t)
          v = t.toLocalDateTime().toString().replace('T', ' ');
        row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), v);
      }
      return row;
    });
  }

  protected Map<String, Object> one(String sql, Map<String, ?> params) {
    var rows = rows(sql, params);
    if (rows.isEmpty())
      throw new NotFoundException("Record not found.");
    return rows.getFirst();
  }

  protected long count(String sql, Map<String, ?> params) {
    return jdbc.queryForObject(sql, params, Long.class);
  }

  protected int update(String sql, Map<String, ?> params) {
    return jdbc.update(sql, params);
  }
}

package com.restaurant.checkout.repository;

import org.springframework.stereotype.Repository;

/** Menu persistence uses the same approved schema as orders and reporting. */
@Repository
public class MenuRepository extends CheckoutRepository {
  public MenuRepository(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc) {
    super(jdbc);
  }

  public static final String SELECT =
      "SELECT m.item_id AS id,m.item_name AS name,c.category_name AS"
          + " category,m.description,m.unit_price AS price,m.is_available AS available,m.is_active"
          + " FROM Menu_item m JOIN Menu_category c ON c.category_id=m.category_id ";
}

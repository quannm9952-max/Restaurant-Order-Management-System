package com.restaurant.repository.support;

/** Escapes SQL LIKE metacharacters using the explicit ! escape character. */
public final class SqlSearch {
  private SqlSearch() {
  }
  public static String containsLiteral(String value) {
    return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }
}

package com.restaurant.entity.enums;

import java.util.Locale;

/**
 * UNKNOWN groups unclassified legacy records; new payments use the other two
 * values.
 */
public enum PaymentMethod {
  CASH("Cash"), BANK_TRANSFER("Bank Transfer"), UNKNOWN("Unknown");

  private final String label;

  PaymentMethod(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static PaymentMethod forPayment(String value) {
    if ("CASH".equals(value))
      return CASH;
    if ("BANK_TRANSFER".equals(value))
      return BANK_TRANSFER;
    throw new IllegalArgumentException("Choose Cash or Bank Transfer.");
  }

  public static PaymentMethod classify(String value) {
    String normalized = value == null
        ? ""
        : value.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "CASH" -> CASH;
      case "BANK_TRANSFER", "BANK TRANSFER" -> BANK_TRANSFER;
      default -> UNKNOWN;
    };
  }

  public static String display(String value) {
    PaymentMethod method = classify(value);
    return method != UNKNOWN
        ? method.label()
        : value == null || value.isBlank() ? UNKNOWN.label() : value.trim();
  }
}

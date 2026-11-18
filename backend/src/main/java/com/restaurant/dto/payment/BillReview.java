package com.restaurant.dto.payment;

import java.math.BigDecimal;
import java.util.List;

/** Reviewed quantities and the order snapshot displayed to the cashier. */
public record BillReview(List<BillLineQuantity> lines, String revision,
    BigDecimal expectedTotal) {
}

package com.restaurant.dto.payment;

/** Quantity reviewed by the cashier before the bill is generated. */
public record BillLineQuantity(Long detailId, Integer quantity) {
}

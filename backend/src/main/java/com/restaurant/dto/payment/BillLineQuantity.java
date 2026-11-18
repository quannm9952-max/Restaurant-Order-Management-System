package com.restaurant.dto.payment;

/** Quantity reviewed by the cashier before payment. */
public record BillLineQuantity(Long detailId, Integer quantity) {
}

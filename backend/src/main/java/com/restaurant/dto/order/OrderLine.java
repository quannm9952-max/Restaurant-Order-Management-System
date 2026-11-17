package com.restaurant.dto.order;

/** Item and quantity supplied to order creation or addition. */
public record OrderLine(Integer itemId, Integer quantity) {
}

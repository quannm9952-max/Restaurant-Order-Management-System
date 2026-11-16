package com.example.restaurantmanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Form DTO for a single order line item (used in CreateOrderForm and AddItemForm).
 */
public class OrderLineForm {

    @NotNull(message = "Phải chọn món.")
    private Integer itemId;

    @NotNull(message = "Phải nhập số lượng.")
    @Min(value = 1, message = "Số lượng phải >= 1.")
    private Integer quantity;

    public OrderLineForm() {
    }

    public OrderLineForm(Integer itemId, Integer quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

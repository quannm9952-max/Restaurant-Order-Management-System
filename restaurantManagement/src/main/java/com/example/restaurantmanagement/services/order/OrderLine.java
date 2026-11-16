package com.example.restaurantmanagement.services.order;

/**
 * Simple DTO for order line items during creation/addition.
 * Used by IOrderService methods to pass item ID and quantity.
 */
public class OrderLine {
    private Integer itemId;
    private Integer quantity;

    public OrderLine() {
    }

    public OrderLine(Integer itemId, Integer quantity) {
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

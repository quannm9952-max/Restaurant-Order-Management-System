package com.restaurant.dto.order;

import com.restaurant.entities.enums.CookingStatus;
import java.math.BigDecimal;

/** View DTO for a single order detail line in the order detail view. */
public record OrderLineView(
    Integer detailId,
    String itemNameAtOrder,
    Integer quantity,
    BigDecimal unitPriceAtOrder,
    BigDecimal subtotal,
    CookingStatus cookingStatus,
    boolean canEdit) {}

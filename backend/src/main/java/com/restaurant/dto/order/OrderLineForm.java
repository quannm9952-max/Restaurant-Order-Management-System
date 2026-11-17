package com.restaurant.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Form DTO for a single order line item (used in CreateOrderForm and
 * AddItemForm).
 */
@lombok.Getter
@lombok.Setter
public class OrderLineForm {

  @NotNull(message = "Phải chọn món.")
  private Integer itemId;

  @NotNull(message = "Phải nhập số lượng.")
  @Min(value = 1, message = "Số lượng phải >= 1.")
  private Integer quantity;

}

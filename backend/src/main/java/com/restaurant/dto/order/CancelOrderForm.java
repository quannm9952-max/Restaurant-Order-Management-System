package com.restaurant.dto.order;

import jakarta.validation.constraints.Size;

/** Form DTO for canceling an entire order. */
@lombok.Getter
@lombok.Setter
public class CancelOrderForm {

  @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự.")
  private String reason;

}

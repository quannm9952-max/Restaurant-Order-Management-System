package com.restaurant.dto;

import jakarta.validation.constraints.Size;

/** Form DTO for canceling an entire order. */
public class CancelOrderForm {

  @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự.")
  private String reason;

  public CancelOrderForm() {}

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}

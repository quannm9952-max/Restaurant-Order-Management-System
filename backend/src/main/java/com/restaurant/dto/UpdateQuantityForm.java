package com.restaurant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Form DTO for reducing the quantity of an order detail line. */
public class UpdateQuantityForm {

  @NotNull(message = "Phải nhập số lượng mới.")
  @Min(value = 1, message = "Số lượng mới phải >= 1.")
  private Integer newQuantity;

  public UpdateQuantityForm() {
  }

  public Integer getNewQuantity() {
    return newQuantity;
  }

  public void setNewQuantity(Integer newQuantity) {
    this.newQuantity = newQuantity;
  }
}

package com.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Form DTO for adding items to an existing order. */
public class AddItemForm {

  @NotNull(message = "Phải có ít nhất 1 dòng món để thêm.")
  @Valid
  private List<OrderLineForm> lines;

  public AddItemForm() {}

  public List<OrderLineForm> getLines() {
    return lines;
  }

  public void setLines(List<OrderLineForm> lines) {
    this.lines = lines;
  }
}

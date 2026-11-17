package com.restaurant.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Form DTO for adding items to an existing order. */
@lombok.Getter
@lombok.Setter
public class AddItemForm {

  @NotNull(message = "Phải có ít nhất 1 dòng món để thêm.")
  private List<@Valid OrderLineForm> lines;

}

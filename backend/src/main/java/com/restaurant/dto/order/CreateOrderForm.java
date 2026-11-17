package com.restaurant.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Form DTO for creating a new order. */
@lombok.Getter
@lombok.Setter
public class CreateOrderForm {

  @NotNull(message = "Phải chọn bàn.")
  private Integer tableId;

  @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
  private String note;

  @NotNull(message = "Đơn phải có ít nhất 1 dòng món.")
  private List<@Valid OrderLineForm> lines;

}

package com.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Form DTO for creating a new order. */
public class CreateOrderForm {

  @NotNull(message = "Phải chọn bàn.")
  private Integer tableId;

  @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
  private String note;

  @NotNull(message = "Đơn phải có ít nhất 1 dòng món.")
  @Valid
  private List<OrderLineForm> lines;

  public CreateOrderForm() {
  }

  public Integer getTableId() {
    return tableId;
  }

  public void setTableId(Integer tableId) {
    this.tableId = tableId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<OrderLineForm> getLines() {
    return lines;
  }

  public void setLines(List<OrderLineForm> lines) {
    this.lines = lines;
  }
}

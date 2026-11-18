package com.example.restaurantmanagement.dto.table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for creating a new table.
 */
public record CreateTableForm(
        @NotBlank(message = "Tên bàn không được để trống.")
        @Size(max = 50, message = "Tên bàn tối đa 50 ký tự.")
        String tableName,

        @NotNull(message = "Sức chứa không được để trống.")
        @Max(value = 200, message = "Sức chứa tối đa 200 chỗ.")
        Integer capacity
) {
}

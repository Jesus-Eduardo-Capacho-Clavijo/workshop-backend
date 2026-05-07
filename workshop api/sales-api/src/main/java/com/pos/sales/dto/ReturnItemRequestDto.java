package com.pos.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReturnItemRequestDto(
    @NotNull(message = "Sale item ID is required") Long saleItemId,
    @NotNull(message = "Quantity to return is required")
    @Min(value = 1, message = "Return quantity must be at least 1") Integer quantity,
    @NotBlank(message = "Reason for returning this item is required") String reason
) {}

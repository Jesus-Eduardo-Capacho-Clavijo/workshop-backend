package com.pos.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequestDto(
    String productId,
    String barcode,
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {}

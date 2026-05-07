package com.pos.sales.dto;

import java.math.BigDecimal;

public record SaleItemResponseDto(
    Long id,
    String productId,
    String barcode,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal,
    Integer returnedQuantity
) {}

package com.pos.sales.dto;

import java.math.BigDecimal;

public record ProductDto(
    String id,
    String name,
    String barcode,
    BigDecimal unitPrice,
    Integer availableStock,
    String category
) {}

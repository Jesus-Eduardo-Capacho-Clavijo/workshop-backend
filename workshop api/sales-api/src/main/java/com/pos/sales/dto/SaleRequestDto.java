package com.pos.sales.dto;

import jakarta.validation.constraints.NotBlank;

public record SaleRequestDto(
    @NotBlank(message = "Terminal ID is required") String terminalId,
    String customerId
) {}

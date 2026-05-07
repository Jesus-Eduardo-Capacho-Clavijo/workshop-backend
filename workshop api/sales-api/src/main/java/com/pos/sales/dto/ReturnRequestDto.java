package com.pos.sales.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ReturnRequestDto(
    @NotBlank(message = "Return reason is required") String reason,
    List<ReturnItemRequestDto> items
) {}

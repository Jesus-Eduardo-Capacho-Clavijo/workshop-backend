package com.pos.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequestDto(
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Cancellation reason max length is 255")
    String reason
) {}

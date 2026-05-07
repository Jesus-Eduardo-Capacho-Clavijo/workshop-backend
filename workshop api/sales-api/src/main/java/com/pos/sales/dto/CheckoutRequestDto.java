package com.pos.sales.dto;

import com.pos.sales.model.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CheckoutRequestDto(
    @NotNull(message = "Payment type is required") PaymentType paymentType,
    BigDecimal amountReceived,
    String customerId
) {}

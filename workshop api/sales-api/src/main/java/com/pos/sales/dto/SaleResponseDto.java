package com.pos.sales.dto;

import com.pos.sales.model.enums.PaymentType;
import com.pos.sales.model.enums.SaleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponseDto(
    Long id,
    String terminalId,
    String cashierId,
    String customerId,
    SaleStatus status,
    PaymentType paymentType,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal discount,
    BigDecimal total,
    BigDecimal amountReceived,
    BigDecimal changeAmount,
    String creditReference,
    String transactionId,
    String cancellationReason,
    LocalDateTime completedAt,
    List<SaleItemResponseDto> items
) {}

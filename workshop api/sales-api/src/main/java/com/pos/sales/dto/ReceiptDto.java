package com.pos.sales.dto;

import com.pos.sales.model.enums.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptDto(
    String storeName,
    String terminalId,
    String cashierId,
    LocalDateTime dateTime,
    String customerId,
    String customerName,
    List<SaleItemResponseDto> items,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal discount,
    BigDecimal total,
    PaymentType paymentMethod,
    BigDecimal amountReceived,
    BigDecimal changeAmount,
    String transactionId,
    String creditReference,
    boolean isReturn,
    String originalTransactionId
) {}

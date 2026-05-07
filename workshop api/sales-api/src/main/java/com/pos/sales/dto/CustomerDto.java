package com.pos.sales.dto;

public record CustomerDto(
    String id,
    String fullName,
    String documentType,
    String documentNumber,
    String creditStatus
) {}

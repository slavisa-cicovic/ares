package com.sc.ares.features.item.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponse(
    UUID id,
    UUID createdByUserId,
    String name,
    String code,
    String description,
    String unit,
    BigDecimal price,
    BigDecimal taxRate,
    BigDecimal defaultValue,
    String status
) {
}

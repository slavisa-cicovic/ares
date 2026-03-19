package com.sc.ares.features.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpsertItemRequest(
    @NotBlank
    @Size(max = 255)
    String name,
    @Size(max = 64)
    String code,
    @Size(max = 1000)
    String description,
    @NotBlank
    @Size(max = 50)
    String unit,
    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 17, fraction = 2)
    BigDecimal price,
    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    BigDecimal taxRate,
    @NotBlank
    String status
) {
}

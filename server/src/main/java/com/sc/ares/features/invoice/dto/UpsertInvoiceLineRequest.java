package com.sc.ares.features.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpsertInvoiceLineRequest(
    UUID itemId,
    @NotBlank
    @Size(max = 255)
    String itemName,
    @Size(max = 64)
    String itemCode,
    @NotBlank
    @Size(max = 50)
    String unit,
    @NotNull
    @DecimalMin(value = "0.001")
    @Digits(integer = 12, fraction = 3)
    BigDecimal quantity,
    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 17, fraction = 2)
    BigDecimal unitPrice,
    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    BigDecimal taxRate
) {
}

package com.sc.ares.features.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
    @NotNull
    LocalDate paymentDate,
    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 17, fraction = 2)
    BigDecimal amount,
    @Size(max = 1000)
    String note
) {
}

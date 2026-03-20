package com.sc.ares.features.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    LocalDate paymentDate,
    BigDecimal amount,
    String note
) {
}

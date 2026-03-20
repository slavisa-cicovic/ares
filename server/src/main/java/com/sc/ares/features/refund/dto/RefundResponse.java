package com.sc.ares.features.refund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    LocalDate refundDate,
    BigDecimal amount,
    String note
) {
}

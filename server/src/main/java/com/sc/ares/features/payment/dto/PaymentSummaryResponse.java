package com.sc.ares.features.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentSummaryResponse(
    UUID id,
    LocalDate paymentDate,
    BigDecimal amount,
    String note,
    UUID invoiceId,
    String invoiceNumber,
    LocalDate invoiceIssueDate,
    UUID clientId,
    String clientName,
    String currency
) {
}

package com.sc.ares.features.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
    UUID id,
    String invoiceNumber,
    UUID clientId,
    String clientName,
    LocalDate issueDate,
    LocalDate dueDate,
    String currency,
    String status,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal total
) {
}

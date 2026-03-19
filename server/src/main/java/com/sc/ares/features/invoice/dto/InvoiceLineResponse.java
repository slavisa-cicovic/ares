package com.sc.ares.features.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
    UUID id,
    Integer lineNumber,
    UUID itemId,
    String itemName,
    String itemCode,
    String unit,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal lineSubtotal,
    BigDecimal lineTaxTotal,
    BigDecimal lineTotal
) {
}

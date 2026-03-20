package com.sc.ares.features.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    UUID clientId,
    String clientName,
    LocalDate issueDate,
    LocalDate dueDate,
    String currency,
    String status,
    String documentType,
    UUID originalInvoiceId,
    String originalInvoiceNumber,
    UUID linkedCreditNoteId,
    String linkedCreditNoteNumber,
    String note,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal total,
    String paymentStatus,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    List<InvoiceLineResponse> lines
) {
}

package com.sc.ares.features.reports.dto;

import java.math.BigDecimal;

public record ReportsSummaryResponse(
    long invoiceCount,
    BigDecimal totalInvoicedAmount,
    BigDecimal totalPaidAmount,
    BigDecimal totalOutstandingAmount,
    long unpaidInvoiceCount,
    long partiallyPaidInvoiceCount,
    long paidInvoiceCount
) {
}

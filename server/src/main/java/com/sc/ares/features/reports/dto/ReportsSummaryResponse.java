package com.sc.ares.features.reports.dto;

import java.math.BigDecimal;

public record ReportsSummaryResponse(
    long invoiceCount,
    BigDecimal totalInvoicedAmount,
    BigDecimal totalPaidAmount,
    BigDecimal totalRefundedAmount,
    BigDecimal totalOutstandingAmount,
    BigDecimal unsettledRefundAmount,
    long unpaidInvoiceCount,
    long partiallyPaidInvoiceCount,
    long paidInvoiceCount
) {
}

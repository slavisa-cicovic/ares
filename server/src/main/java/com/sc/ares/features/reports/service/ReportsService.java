package com.sc.ares.features.reports.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoicePaymentStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.invoice.service.InvoiceAccessService;
import com.sc.ares.features.invoice.service.InvoicePaymentStateService;
import com.sc.ares.features.invoice.service.InvoiceRefundStateService;
import com.sc.ares.features.reports.dto.ReportsSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

    private static final LocalDate MIN_FILTER_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_FILTER_DATE = LocalDate.of(2999, 12, 31);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceAccessService invoiceAccessService;
    private final InvoicePaymentStateService invoicePaymentStateService;
    private final InvoiceRefundStateService invoiceRefundStateService;

    public ReportsService(
        InvoiceRepository invoiceRepository,
        InvoiceAccessService invoiceAccessService,
        InvoicePaymentStateService invoicePaymentStateService,
        InvoiceRefundStateService invoiceRefundStateService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceAccessService = invoiceAccessService;
        this.invoicePaymentStateService = invoicePaymentStateService;
        this.invoiceRefundStateService = invoiceRefundStateService;
    }

    @Transactional(readOnly = true)
    public ReportsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        validateRange(from, to);

        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        List<Invoice> invoices = invoiceRepository.findAllByCompanyIdAndFilters(
            companyId,
            null,
            "",
            from != null,
            normalizeFromFilter(from),
            to != null,
            normalizeToFilter(to),
            Sort.by(Sort.Direction.DESC, "issueDate")
        ).stream()
            .filter(invoice -> matchesDateRange(invoice, from, to))
            .toList();

        Map<UUID, BigDecimal> paidAmounts = invoicePaymentStateService.getPaidAmounts(invoices);
        Map<UUID, BigDecimal> refundedAmounts = invoiceRefundStateService.getRefundedAmounts(invoices);

        BigDecimal totalInvoicedAmount = money(BigDecimal.ZERO);
        BigDecimal totalPaidAmount = money(BigDecimal.ZERO);
        BigDecimal totalRefundedAmount = money(BigDecimal.ZERO);
        BigDecimal totalOutstandingAmount = money(BigDecimal.ZERO);
        BigDecimal unsettledRefundAmount = money(BigDecimal.ZERO);
        long unpaidInvoiceCount = 0;
        long partiallyPaidInvoiceCount = 0;
        long paidInvoiceCount = 0;

        for (Invoice invoice : invoices) {
            BigDecimal paidAmount = paidAmounts.getOrDefault(invoice.getId(), money(BigDecimal.ZERO));
            BigDecimal refundedAmount = refundedAmounts.getOrDefault(invoice.getId(), money(BigDecimal.ZERO));
            BigDecimal outstandingAmount = invoicePaymentStateService.getOutstandingAmount(invoice, paidAmount);
            InvoicePaymentStatus paymentStatus = invoicePaymentStateService.derivePaymentStatus(invoice, paidAmount);
            BigDecimal remainingRefundAmount = invoiceRefundStateService.getRemainingRefundAmount(invoice, refundedAmount);

            totalInvoicedAmount = money(totalInvoicedAmount.add(invoice.getTotal()));
            totalPaidAmount = money(totalPaidAmount.add(paidAmount));
            totalRefundedAmount = money(totalRefundedAmount.add(refundedAmount));
            totalOutstandingAmount = money(totalOutstandingAmount.add(outstandingAmount));
            unsettledRefundAmount = money(unsettledRefundAmount.add(remainingRefundAmount));

            switch (paymentStatus) {
                case UNPAID -> unpaidInvoiceCount++;
                case PARTIALLY_PAID -> partiallyPaidInvoiceCount++;
                case PAID -> paidInvoiceCount++;
            }
        }

        totalPaidAmount = money(totalPaidAmount.subtract(totalRefundedAmount));

        if (totalPaidAmount.signum() < 0) {
            totalPaidAmount = money(BigDecimal.ZERO);
        }

        totalOutstandingAmount = money(totalInvoicedAmount.subtract(totalPaidAmount).subtract(unsettledRefundAmount));

        if (totalOutstandingAmount.signum() < 0) {
            totalOutstandingAmount = money(BigDecimal.ZERO);
        }

        return new ReportsSummaryResponse(
            invoices.size(),
            totalInvoicedAmount,
            totalPaidAmount,
            totalRefundedAmount,
            totalOutstandingAmount,
            unsettledRefundAmount,
            unpaidInvoiceCount,
            partiallyPaidInvoiceCount,
            paidInvoiceCount
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ApiBadRequestException("Report date range is invalid.");
        }
    }

    private LocalDate normalizeFromFilter(LocalDate from) {
        return from != null ? from : MIN_FILTER_DATE;
    }

    private LocalDate normalizeToFilter(LocalDate to) {
        return to != null ? to : MAX_FILTER_DATE;
    }

    private boolean matchesDateRange(Invoice invoice, LocalDate from, LocalDate to) {
        if (from != null && invoice.getIssueDate().isBefore(from)) {
            return false;
        }

        if (to != null && invoice.getIssueDate().isAfter(to)) {
            return false;
        }

        return true;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

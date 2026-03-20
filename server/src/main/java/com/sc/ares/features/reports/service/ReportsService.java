package com.sc.ares.features.reports.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoicePaymentStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.invoice.service.InvoiceAccessService;
import com.sc.ares.features.invoice.service.InvoicePaymentStateService;
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

    private final InvoiceRepository invoiceRepository;
    private final InvoiceAccessService invoiceAccessService;
    private final InvoicePaymentStateService invoicePaymentStateService;

    public ReportsService(
        InvoiceRepository invoiceRepository,
        InvoiceAccessService invoiceAccessService,
        InvoicePaymentStateService invoicePaymentStateService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceAccessService = invoiceAccessService;
        this.invoicePaymentStateService = invoicePaymentStateService;
    }

    @Transactional(readOnly = true)
    public ReportsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        validateRange(from, to);

        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        List<Invoice> invoices = invoiceRepository.findAllByCompanyIdAndFilters(
            companyId,
            null,
            "",
            Sort.by(Sort.Direction.DESC, "issueDate")
        ).stream()
            .filter(invoice -> matchesDateRange(invoice, from, to))
            .toList();

        Map<UUID, BigDecimal> paidAmounts = invoicePaymentStateService.getPaidAmounts(invoices);

        BigDecimal totalInvoicedAmount = money(BigDecimal.ZERO);
        BigDecimal totalPaidAmount = money(BigDecimal.ZERO);
        BigDecimal totalOutstandingAmount = money(BigDecimal.ZERO);
        long unpaidInvoiceCount = 0;
        long partiallyPaidInvoiceCount = 0;
        long paidInvoiceCount = 0;

        for (Invoice invoice : invoices) {
            BigDecimal paidAmount = paidAmounts.getOrDefault(invoice.getId(), money(BigDecimal.ZERO));
            BigDecimal outstandingAmount = invoicePaymentStateService.getOutstandingAmount(invoice, paidAmount);
            InvoicePaymentStatus paymentStatus = invoicePaymentStateService.derivePaymentStatus(invoice, paidAmount);

            totalInvoicedAmount = money(totalInvoicedAmount.add(invoice.getTotal()));
            totalPaidAmount = money(totalPaidAmount.add(paidAmount));
            totalOutstandingAmount = money(totalOutstandingAmount.add(outstandingAmount));

            switch (paymentStatus) {
                case UNPAID -> unpaidInvoiceCount++;
                case PARTIALLY_PAID -> partiallyPaidInvoiceCount++;
                case PAID -> paidInvoiceCount++;
            }
        }

        return new ReportsSummaryResponse(
            invoices.size(),
            totalInvoicedAmount,
            totalPaidAmount,
            totalOutstandingAmount,
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

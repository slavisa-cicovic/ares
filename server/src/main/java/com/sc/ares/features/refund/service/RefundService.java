package com.sc.ares.features.refund.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.invoice.service.InvoiceAccessService;
import com.sc.ares.features.invoice.service.InvoiceRefundStateService;
import com.sc.ares.features.refund.dto.CreateRefundRequest;
import com.sc.ares.features.refund.dto.RefundResponse;
import com.sc.ares.features.refund.entity.CreditNoteRefund;
import com.sc.ares.features.refund.repository.CreditNoteRefundRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private final CreditNoteRefundRepository creditNoteRefundRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAccessService invoiceAccessService;
    private final InvoiceRefundStateService invoiceRefundStateService;

    public RefundService(
        CreditNoteRefundRepository creditNoteRefundRepository,
        InvoiceRepository invoiceRepository,
        InvoiceAccessService invoiceAccessService,
        InvoiceRefundStateService invoiceRefundStateService
    ) {
        this.creditNoteRefundRepository = creditNoteRefundRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceAccessService = invoiceAccessService;
        this.invoiceRefundStateService = invoiceRefundStateService;
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> listRefunds(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureCreditNote(invoice);

        return creditNoteRefundRepository.findAllByInvoice_IdAndCompany_IdOrderByRefundDateDescCreatedAtDesc(invoiceId, companyId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public RefundResponse createRefund(UUID invoiceId, CreateRefundRequest request) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureCreditNote(invoice);

        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal refundedAmount = invoiceRefundStateService.getRefundedAmount(invoice);
        BigDecimal remainingRefundAmount = invoiceRefundStateService.getRemainingRefundAmount(invoice, refundedAmount);

        if (remainingRefundAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessConflictException("Credit note does not require any refund.");
        }

        if (amount.compareTo(remainingRefundAmount) > 0) {
            throw new BusinessConflictException("Refund amount exceeds remaining refundable amount.");
        }

        CreditNoteRefund refund = CreditNoteRefund.create(
            invoice.getCompany(),
            invoice,
            request.refundDate(),
            amount,
            normalizeOptional(request.note())
        );

        return toResponse(creditNoteRefundRepository.save(refund));
    }

    private Invoice getRequiredInvoice(UUID invoiceId, UUID companyId) {
        return invoiceRepository.findByIdAndCompanyIdAndDeletedAtIsNull(invoiceId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }

    private void ensureCreditNote(Invoice invoice) {
        if (invoice.getDocumentType() != InvoiceDocumentType.CREDIT_NOTE) {
            throw new BusinessConflictException("Refunds can only be recorded for credit notes.");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ApiBadRequestException("Required refund field is missing.");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private RefundResponse toResponse(CreditNoteRefund refund) {
        return new RefundResponse(
            refund.getId(),
            refund.getRefundDate(),
            refund.getAmount(),
            refund.getNote()
        );
    }
}

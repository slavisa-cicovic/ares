package com.sc.ares.features.payment.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.invoice.service.InvoiceAccessService;
import com.sc.ares.features.payment.dto.CreatePaymentRequest;
import com.sc.ares.features.payment.dto.PaymentResponse;
import com.sc.ares.features.payment.entity.Payment;
import com.sc.ares.features.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAccessService invoiceAccessService;

    public PaymentService(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        InvoiceAccessService invoiceAccessService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceAccessService = invoiceAccessService;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        getRequiredInvoice(invoiceId, companyId);

        return paymentRepository.findAllByInvoice_IdAndCompany_IdOrderByPaymentDateDescCreatedAtDesc(invoiceId, companyId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public PaymentResponse createPayment(UUID invoiceId, CreatePaymentRequest request) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureIssued(invoice);

        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal currentPaidAmount = paidAmount(invoiceId, companyId);
        BigDecimal nextPaidAmount = currentPaidAmount.add(amount).setScale(2, RoundingMode.HALF_UP);

        if (nextPaidAmount.compareTo(invoice.getTotal()) > 0) {
            throw new BusinessConflictException("Payment amount exceeds outstanding invoice amount.");
        }

        Payment payment = Payment.create(
            invoice,
            request.paymentDate(),
            amount,
            normalizeOptional(request.note())
        );

        return toResponse(paymentRepository.save(payment));
    }

    private Invoice getRequiredInvoice(UUID invoiceId, UUID companyId) {
        return invoiceRepository.findByIdAndCompanyIdAndDeletedAtIsNull(invoiceId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }

    private void ensureIssued(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessConflictException("Payments can only be recorded for issued invoices.");
        }

        if (invoice.getDocumentType() != InvoiceDocumentType.INVOICE) {
            throw new BusinessConflictException("Payments cannot be recorded for credit notes.");
        }
    }

    private BigDecimal paidAmount(UUID invoiceId, UUID companyId) {
        return paymentRepository.sumAmountByInvoiceIdAndCompanyId(invoiceId, companyId)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ApiBadRequestException("Required payment field is missing.");
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

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getNote()
        );
    }
}

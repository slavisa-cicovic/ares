package com.sc.ares.features.invoice.service;

import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoiceRefundStatus;
import com.sc.ares.features.refund.repository.CreditNoteRefundRepository;
import com.sc.ares.features.refund.repository.InvoiceRefundAmountProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvoiceRefundStateService {

    private final CreditNoteRefundRepository creditNoteRefundRepository;
    private final InvoicePaymentStateService invoicePaymentStateService;

    public InvoiceRefundStateService(
        CreditNoteRefundRepository creditNoteRefundRepository,
        InvoicePaymentStateService invoicePaymentStateService
    ) {
        this.creditNoteRefundRepository = creditNoteRefundRepository;
        this.invoicePaymentStateService = invoicePaymentStateService;
    }

    public Map<UUID, BigDecimal> getRefundedAmounts(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, BigDecimal> refundedAmounts = new HashMap<>();

        for (InvoiceRefundAmountProjection projection : creditNoteRefundRepository.sumAmountsByInvoiceIds(
            invoices.stream().map(Invoice::getId).toList()
        )) {
            refundedAmounts.put(projection.getInvoiceId(), money(projection.getRefundedAmount()));
        }

        return refundedAmounts;
    }

    public BigDecimal getRefundedAmount(Invoice invoice) {
        return money(creditNoteRefundRepository.sumAmountByInvoiceIdAndCompanyId(invoice.getId(), invoice.getCompany().getId()));
    }

    public BigDecimal getRefundableAmount(Invoice invoice) {
        if (invoice.getDocumentType() != InvoiceDocumentType.CREDIT_NOTE || invoice.getOriginalInvoice() == null) {
            return money(BigDecimal.ZERO);
        }

        Invoice originalInvoice = invoice.getOriginalInvoice();
        BigDecimal originalPaidAmount = invoicePaymentStateService.getPaidAmount(originalInvoice);
        BigDecimal reversedAmount = money(invoice.getTotal().abs());
        BigDecimal remainingReceivable = money(originalInvoice.getTotal().subtract(reversedAmount));

        if (remainingReceivable.signum() < 0) {
            remainingReceivable = money(BigDecimal.ZERO);
        }

        BigDecimal refundableAmount = money(originalPaidAmount.subtract(remainingReceivable));

        if (refundableAmount.signum() < 0) {
            return money(BigDecimal.ZERO);
        }

        return refundableAmount;
    }

    public BigDecimal getRemainingRefundAmount(Invoice invoice, BigDecimal refundedAmount) {
        BigDecimal remainingRefundAmount = money(getRefundableAmount(invoice).subtract(refundedAmount));

        if (remainingRefundAmount.signum() < 0) {
            return money(BigDecimal.ZERO);
        }

        return remainingRefundAmount;
    }

    public InvoiceRefundStatus deriveRefundStatus(Invoice invoice, BigDecimal refundableAmount, BigDecimal refundedAmount) {
        if (invoice.getDocumentType() != InvoiceDocumentType.CREDIT_NOTE) {
            return InvoiceRefundStatus.NOT_APPLICABLE;
        }

        if (refundableAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoiceRefundStatus.NOT_REQUIRED;
        }

        if (refundedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoiceRefundStatus.PENDING_REFUND;
        }

        if (refundedAmount.compareTo(refundableAmount) >= 0) {
            return InvoiceRefundStatus.REFUNDED;
        }

        return InvoiceRefundStatus.PARTIALLY_REFUNDED;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

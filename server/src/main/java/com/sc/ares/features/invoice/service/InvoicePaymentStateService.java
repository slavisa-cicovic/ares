package com.sc.ares.features.invoice.service;

import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoicePaymentStatus;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.payment.repository.InvoicePaidAmountProjection;
import com.sc.ares.features.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentStateService {

    private final PaymentRepository paymentRepository;

    public InvoicePaymentStateService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Map<UUID, BigDecimal> getPaidAmounts(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, BigDecimal> paidAmounts = new HashMap<>();

        for (InvoicePaidAmountProjection projection : paymentRepository.sumAmountsByInvoiceIds(
            invoices.stream().map(Invoice::getId).toList()
        )) {
            paidAmounts.put(projection.getInvoiceId(), money(projection.getPaidAmount()));
        }

        return paidAmounts;
    }

    public BigDecimal getPaidAmount(Invoice invoice) {
        return money(paymentRepository.sumAmountByInvoiceIdAndCompanyId(invoice.getId(), invoice.getCompany().getId()));
    }

    public BigDecimal getOutstandingAmount(Invoice invoice, BigDecimal paidAmount) {
        if (invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE) {
            return money(BigDecimal.ZERO);
        }

        BigDecimal outstandingAmount = money(invoice.getTotal().subtract(paidAmount));

        if (outstandingAmount.signum() < 0) {
            return money(BigDecimal.ZERO);
        }

        return outstandingAmount;
    }

    public InvoicePaymentStatus derivePaymentStatus(Invoice invoice, BigDecimal paidAmount) {
        if (invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE) {
            return InvoicePaymentStatus.PAID;
        }

        if (invoice.getStatus() == InvoiceStatus.DRAFT || paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoicePaymentStatus.UNPAID;
        }

        if (paidAmount.compareTo(invoice.getTotal()) >= 0) {
            return InvoicePaymentStatus.PAID;
        }

        return InvoicePaymentStatus.PARTIALLY_PAID;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

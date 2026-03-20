package com.sc.ares.features.refund.entity;

import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.invoice.entity.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_note_refunds")
public class CreditNoteRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "refund_date", nullable = false)
    private LocalDate refundDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CreditNoteRefund() {
    }

    public static CreditNoteRefund create(
        Company company,
        Invoice invoice,
        LocalDate refundDate,
        BigDecimal amount,
        String note
    ) {
        Instant now = Instant.now();

        CreditNoteRefund refund = new CreditNoteRefund();
        refund.company = company;
        refund.invoice = invoice;
        refund.refundDate = refundDate;
        refund.amount = amount;
        refund.note = note;
        refund.createdAt = now;
        refund.updatedAt = now;

        return refund;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getRefundDate() {
        return refundDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}

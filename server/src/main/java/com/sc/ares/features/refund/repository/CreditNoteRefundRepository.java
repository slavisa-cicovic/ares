package com.sc.ares.features.refund.repository;

import com.sc.ares.features.refund.entity.CreditNoteRefund;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CreditNoteRefundRepository extends JpaRepository<CreditNoteRefund, UUID> {

    List<CreditNoteRefund> findAllByInvoice_IdAndCompany_IdOrderByRefundDateDescCreatedAtDesc(UUID invoiceId, UUID companyId);

    @Query("""
        select coalesce(sum(refund.amount), 0)
        from CreditNoteRefund refund
        where refund.invoice.id = :invoiceId
          and refund.company.id = :companyId
        """)
    BigDecimal sumAmountByInvoiceIdAndCompanyId(UUID invoiceId, UUID companyId);

    @Query("""
        select refund.invoice.id as invoiceId, coalesce(sum(refund.amount), 0) as refundedAmount
        from CreditNoteRefund refund
        where refund.invoice.id in :invoiceIds
        group by refund.invoice.id
        """)
    List<InvoiceRefundAmountProjection> sumAmountsByInvoiceIds(List<UUID> invoiceIds);
}

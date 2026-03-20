package com.sc.ares.features.payment.repository;

import com.sc.ares.features.payment.entity.Payment;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByInvoice_IdAndCompany_IdOrderByPaymentDateDescCreatedAtDesc(UUID invoiceId, UUID companyId);

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from Payment payment
        where payment.invoice.id = :invoiceId
          and payment.company.id = :companyId
        """)
    BigDecimal sumAmountByInvoiceIdAndCompanyId(UUID invoiceId, UUID companyId);

    @Query("""
        select payment.invoice.id as invoiceId, coalesce(sum(payment.amount), 0) as paidAmount
        from Payment payment
        where payment.invoice.id in :invoiceIds
        group by payment.invoice.id
        """)
    List<InvoicePaidAmountProjection> sumAmountsByInvoiceIds(List<UUID> invoiceIds);
}

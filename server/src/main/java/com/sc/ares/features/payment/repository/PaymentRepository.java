package com.sc.ares.features.payment.repository;

import com.sc.ares.features.payment.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByInvoice_IdAndNote(UUID invoiceId, String note);

    List<Payment> findAllByInvoice_IdAndCompany_IdOrderByPaymentDateDescCreatedAtDesc(UUID invoiceId, UUID companyId);

    @Query("""
        select payment
        from Payment payment
        join payment.invoice invoice
        join invoice.client client
        where payment.company.id = :companyId
          and (:hasFrom = false or payment.paymentDate >= :from)
          and (:hasTo = false or payment.paymentDate <= :to)
          and (
              :search = ''
              or upper(invoice.invoiceNumber) like upper(concat('%', :search, '%'))
              or upper(client.name) like upper(concat('%', :search, '%'))
              or upper(coalesce(payment.note, '')) like upper(concat('%', :search, '%'))
          )
        """)
    List<Payment> findAllByCompanyIdAndFilters(
        UUID companyId,
        String search,
        boolean hasFrom,
        LocalDate from,
        boolean hasTo,
        LocalDate to,
        Sort sort
    );

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

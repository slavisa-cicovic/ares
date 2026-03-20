package com.sc.ares.features.invoice.repository;

import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndCompanyIdAndDeletedAtIsNull(UUID id, UUID companyId);

    Optional<Invoice> findByCompanyIdAndInvoiceNumberAndDeletedAtIsNull(UUID companyId, String invoiceNumber);

    Optional<Invoice> findByOriginalInvoiceIdAndCompanyIdAndDeletedAtIsNull(UUID originalInvoiceId, UUID companyId);

    Optional<Invoice> findFirstByCompanyIdAndSequenceYearOrderBySequenceNumberDesc(UUID companyId, int sequenceYear);

    @Query("""
        select invoice
        from Invoice invoice
        join invoice.client client
        where invoice.company.id = :companyId
          and invoice.deletedAt is null
          and (:status is null or invoice.status = :status)
          and (:hasFrom = false or invoice.issueDate >= :from)
          and (:hasTo = false or invoice.issueDate <= :to)
          and (
              :search = ''
              or upper(invoice.invoiceNumber) like upper(concat('%', :search, '%'))
              or upper(client.name) like upper(concat('%', :search, '%'))
          )
        """)
    List<Invoice> findAllByCompanyIdAndFilters(
        UUID companyId,
        InvoiceStatus status,
        String search,
        boolean hasFrom,
        LocalDate from,
        boolean hasTo,
        LocalDate to,
        Sort sort
    );
}

package com.sc.ares.features.invoice.repository;

import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndCompanyIdAndDeletedAtIsNull(UUID id, UUID companyId);

    Optional<Invoice> findFirstByCompanyIdAndSequenceYearOrderBySequenceNumberDesc(UUID companyId, int sequenceYear);

    @Query("""
        select invoice
        from Invoice invoice
        join invoice.client client
        where invoice.company.id = :companyId
          and invoice.deletedAt is null
          and (:status is null or invoice.status = :status)
          and (
              :search is null
              or upper(invoice.invoiceNumber) like upper(concat('%', :search, '%'))
              or upper(client.name) like upper(concat('%', :search, '%'))
          )
        """)
    Page<Invoice> findPageByCompanyIdAndFilters(
        UUID companyId,
        InvoiceStatus status,
        String search,
        Pageable pageable
    );
}

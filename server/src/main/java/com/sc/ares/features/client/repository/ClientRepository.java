package com.sc.ares.features.client.repository;

import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.entity.ClientStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndCompanyIdAndDeletedAtIsNull(UUID id, UUID companyId);

    @Query("""
        select client
        from Client client
        where client.company.id = :companyId
          and client.deletedAt is null
          and (:status is null or client.status = :status)
          and (
              :search is null
              or upper(client.name) like upper(concat('%', :search, '%'))
              or upper(client.email) like upper(concat('%', :search, '%'))
              or upper(client.pib) like upper(concat('%', :search, '%'))
          )
        """)
    Page<Client> findPageByCompanyIdAndFilters(
        UUID companyId,
        ClientStatus status,
        String search,
        Pageable pageable
    );
}

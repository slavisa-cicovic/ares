package com.sc.ares.features.company.repository;

import com.sc.ares.features.company.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByNameAndDeletedAtIsNull(String name);

    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByPibAndDeletedAtIsNull(String pib);

    boolean existsByPibAndDeletedAtIsNullAndIdNot(String pib, UUID id);

    boolean existsByMbAndDeletedAtIsNull(String mb);

    boolean existsByMbAndDeletedAtIsNullAndIdNot(String mb, UUID id);

    @Query("""
        select company
        from Company company
        where company.deletedAt is null
          and (
              :search = ''
              or upper(company.name) like upper(concat('%', :search, '%'))
              or upper(company.pib) like upper(concat('%', :search, '%'))
              or upper(company.mb) like upper(concat('%', :search, '%'))
          )
        """)
    Page<Company> findPageByPlatformFilters(String search, Pageable pageable);
}

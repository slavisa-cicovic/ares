package com.sc.ares.features.item.repository;

import com.sc.ares.features.item.entity.Item;
import com.sc.ares.features.item.entity.ItemStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByIdAndCompanyIdAndDeletedAtIsNull(UUID id, UUID companyId);

    List<Item> findAllByCompanyIdAndDeletedAtIsNullOrderByNameAsc(UUID companyId);

    Optional<Item> findByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID companyId, String code);

    boolean existsByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID companyId, String code);

    boolean existsByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID companyId, String code, UUID id);

    @Query("""
        select item
        from Item item
        where item.company.id = :companyId
          and item.deletedAt is null
          and (:status is null or item.status = :status)
          and (
              :search = ''
              or upper(item.name) like upper(concat('%', :search, '%'))
              or upper(item.code) like upper(concat('%', :search, '%'))
          )
        """)
    Page<Item> findPageByCompanyIdAndFilters(
        UUID companyId,
        ItemStatus status,
        String search,
        Pageable pageable
    );
}

package com.sc.ares.features.user.repository;

import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(String email, UUID id);

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByIdAndCompanyIdAndDeletedAtIsNull(UUID id, UUID companyId);

    @Query("""
        select user
        from AppUser user
        where user.company.id = :companyId
          and user.deletedAt is null
          and (:status is null or user.status = :status)
          and (
              :search is null
              or upper(user.email) like upper(concat('%', :search, '%'))
              or upper(user.firstName) like upper(concat('%', :search, '%'))
              or upper(user.lastName) like upper(concat('%', :search, '%'))
          )
        """)
    Page<AppUser> findPageByCompanyIdAndFilters(
        UUID companyId,
        UserStatus status,
        String search,
        Pageable pageable
    );
}

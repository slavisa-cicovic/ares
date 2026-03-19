package com.sc.ares.features.user.repository;

import com.sc.ares.features.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByIdAndDeletedAtIsNull(UUID id);
}

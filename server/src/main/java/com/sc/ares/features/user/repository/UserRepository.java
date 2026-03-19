package com.sc.ares.features.user.repository;

import com.sc.ares.features.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}

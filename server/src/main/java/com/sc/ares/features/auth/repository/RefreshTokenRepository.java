package com.sc.ares.features.auth.repository;

import com.sc.ares.features.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = {"user", "user.company"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}

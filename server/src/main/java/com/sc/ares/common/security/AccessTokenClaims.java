package com.sc.ares.common.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccessTokenClaims(
    UUID tokenId,
    UUID userId,
    UUID companyId,
    String email,
    boolean platformAdmin,
    List<String> roleCodes,
    Instant issuedAt,
    Instant expiresAt
) {
}

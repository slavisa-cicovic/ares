package com.sc.ares.features.auth.service;

import java.time.Instant;

public record IssuedRefreshToken(
    String token,
    Instant expiresAt
) {
}

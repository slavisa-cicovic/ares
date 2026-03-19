package com.sc.ares.common.security;

import java.time.Instant;

public record IssuedAccessToken(
    String token,
    Instant expiresAt
) {
}

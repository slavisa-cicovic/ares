package com.sc.ares.features.auth.dto;

public record AuthTokenResponse(
    String accessToken,
    String refreshToken,
    String accessTokenExpiresAt,
    String refreshTokenExpiresAt,
    AuthenticatedUserResponse user
) {
}

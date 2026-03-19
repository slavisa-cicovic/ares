package com.sc.ares.features.auth.service;

import com.sc.ares.config.AuthTokenProperties;
import com.sc.ares.features.auth.entity.RefreshToken;
import com.sc.ares.features.auth.repository.RefreshTokenRepository;
import com.sc.ares.features.user.entity.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final AuthTokenProperties authTokenProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
        AuthTokenProperties authTokenProperties,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.authTokenProperties = authTokenProperties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public IssuedRefreshToken issue(AppUser user) {
        Instant expiresAt = Instant.now().plus(authTokenProperties.getRefreshTokenTtl());
        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.create(user, hash(rawToken), expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        RefreshToken currentToken = requireUsable(rawToken);
        IssuedRefreshToken rotatedToken = issue(currentToken.getUser());

        RefreshToken replacement = refreshTokenRepository.findByTokenHash(hash(rotatedToken.token()))
            .orElseThrow(() -> new IllegalStateException("Refresh token was not persisted."));

        currentToken.revoke(replacement.getId());
        refreshTokenRepository.save(currentToken);

        return rotatedToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken requireUsable(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

        Instant now = Instant.now();

        if (refreshToken.isRevoked() || refreshToken.isExpired(now)) {
            throw new BadCredentialsException("Invalid refresh token.");
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
            .ifPresent(refreshToken -> {
                if (!refreshToken.isRevoked()) {
                    refreshToken.revoke(null);
                    refreshTokenRepository.save(refreshToken);
                }
            });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash refresh token.", exception);
        }
    }
}

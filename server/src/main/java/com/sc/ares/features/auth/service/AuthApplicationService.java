package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AccessTokenService;
import com.sc.ares.common.security.AresUserDetails;
import com.sc.ares.common.security.IssuedAccessToken;
import com.sc.ares.features.auth.dto.AuthTokenResponse;
import com.sc.ares.features.auth.dto.LoginRequest;
import com.sc.ares.features.auth.dto.LogoutRequest;
import com.sc.ares.features.auth.dto.RefreshTokenRequest;
import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import com.sc.ares.features.auth.entity.RefreshToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final AuthenticationManager authenticationManager;
    private final CurrentPrincipalService currentPrincipalService;
    private final DatabaseUserDetailsService databaseUserDetailsService;
    private final AuthResponseFactory authResponseFactory;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthApplicationService(
        AuthenticationManager authenticationManager,
        CurrentPrincipalService currentPrincipalService,
        DatabaseUserDetailsService databaseUserDetailsService,
        AuthResponseFactory authResponseFactory,
        AccessTokenService accessTokenService,
        RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.currentPrincipalService = currentPrincipalService;
        this.databaseUserDetailsService = databaseUserDetailsService;
        this.authResponseFactory = authResponseFactory;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        AresUserDetails userDetails = extractUserDetails(authentication);
        IssuedAccessToken accessToken = accessTokenService.issue(userDetails);
        IssuedRefreshToken refreshToken = refreshTokenService.issue(
            databaseUserDetailsService.getRequiredUserEntity(userDetails.getPrincipal().userId())
        );

        return authResponseFactory.tokenResponse(userDetails, accessToken, refreshToken);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.requireUsable(request.refreshToken());
        AresUserDetails userDetails = databaseUserDetailsService.loadCurrentUserById(refreshToken.getUser().getId());

        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("Invalid refresh token.");
        }

        IssuedAccessToken accessToken = accessTokenService.issue(userDetails);
        IssuedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(request.refreshToken());

        return authResponseFactory.tokenResponse(userDetails, accessToken, rotatedRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    public AuthenticatedUserResponse getCurrentUser() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();
        AresUserDetails userDetails = databaseUserDetailsService.loadCurrentUserById(principal.userId());

        return authResponseFactory.fromUserDetails(userDetails);
    }

    private AresUserDetails extractUserDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AresUserDetails userDetails) {
            return userDetails;
        }

        throw new InsufficientAuthenticationException("Authenticated user details are required.");
    }
}

package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AresUserDetails;
import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import com.sc.ares.features.auth.dto.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService {

    private final AuthenticationManager authenticationManager;
    private final CurrentPrincipalService currentPrincipalService;
    private final DatabaseUserDetailsService databaseUserDetailsService;
    private final AuthResponseFactory authResponseFactory;

    public AuthApplicationService(
        AuthenticationManager authenticationManager,
        CurrentPrincipalService currentPrincipalService,
        DatabaseUserDetailsService databaseUserDetailsService,
        AuthResponseFactory authResponseFactory
    ) {
        this.authenticationManager = authenticationManager;
        this.currentPrincipalService = currentPrincipalService;
        this.databaseUserDetailsService = databaseUserDetailsService;
        this.authResponseFactory = authResponseFactory;
    }

    public AuthenticatedUserResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
        );

        return authResponseFactory.fromUserDetails(extractUserDetails(authentication));
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

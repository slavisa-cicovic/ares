package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AresUserDetails;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentPrincipalService {

    public AresAuthenticatedPrincipal getRequiredPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new InsufficientAuthenticationException("Authenticated principal is required.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AresAuthenticatedPrincipal authenticatedPrincipal) {
            return authenticatedPrincipal;
        }

        if (principal instanceof AresUserDetails userDetails) {
            return userDetails.getPrincipal();
        }

        throw new InsufficientAuthenticationException("Authenticated principal is required.");
    }
}

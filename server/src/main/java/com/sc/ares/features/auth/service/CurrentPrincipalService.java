package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentPrincipalService {

    public AresAuthenticatedPrincipal getRequiredPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AresAuthenticatedPrincipal principal)) {
            throw new InsufficientAuthenticationException("Authenticated principal is required.");
        }

        return principal;
    }
}

package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PlatformAdminAccessService {

    private final CurrentPrincipalService currentPrincipalService;

    public PlatformAdminAccessService(CurrentPrincipalService currentPrincipalService) {
        this.currentPrincipalService = currentPrincipalService;
    }

    public AresAuthenticatedPrincipal getRequiredPlatformAdminPrincipal() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (!principal.platformAdmin()) {
            throw new AccessDeniedException("Platform admin access is required.");
        }

        return principal;
    }
}

package com.sc.ares.features.invoice.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.features.auth.service.CurrentPrincipalService;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class InvoiceAccessService {

    private final CurrentPrincipalService currentPrincipalService;

    public InvoiceAccessService(CurrentPrincipalService currentPrincipalService) {
        this.currentPrincipalService = currentPrincipalService;
    }

    public UUID getRequiredReadableCompanyId() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (!hasAuthority(principal, "ROLE_COMPANY_ADMIN") && !hasAuthority(principal, "ROLE_USER")) {
            throw new AccessDeniedException("Invoice read access is required.");
        }

        return principal.companyId();
    }

    public UUID getRequiredWritableCompanyId() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (principal.platformAdmin() || !hasAuthority(principal, "ROLE_COMPANY_ADMIN")) {
            throw new AccessDeniedException("Company admin access is required.");
        }

        return principal.companyId();
    }

    private boolean hasAuthority(AresAuthenticatedPrincipal principal, String authority) {
        return principal.authorities().stream()
            .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}

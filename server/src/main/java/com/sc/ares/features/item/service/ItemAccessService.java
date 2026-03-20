package com.sc.ares.features.item.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.features.auth.service.CurrentPrincipalService;
import com.sc.ares.features.item.entity.Item;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ItemAccessService {

    private final CurrentPrincipalService currentPrincipalService;

    public ItemAccessService(CurrentPrincipalService currentPrincipalService) {
        this.currentPrincipalService = currentPrincipalService;
    }

    public UUID getRequiredReadableCompanyId() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (!hasAuthority(principal, "ROLE_COMPANY_ADMIN") && !hasAuthority(principal, "ROLE_USER")) {
            throw new AccessDeniedException("Item read access is required.");
        }

        return principal.companyId();
    }

    public AresAuthenticatedPrincipal getRequiredCreatePrincipal() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (principal.platformAdmin()) {
            throw new AccessDeniedException("Item write access is required.");
        }

        if (!hasAuthority(principal, "ROLE_COMPANY_ADMIN") && !hasAuthority(principal, "ROLE_USER")) {
            throw new AccessDeniedException("Item write access is required.");
        }

        return principal;
    }

    public UUID getRequiredWritableCompanyId() {
        return getRequiredCreatePrincipal().companyId();
    }

    public void ensureCanModify(Item item) {
        AresAuthenticatedPrincipal principal = getRequiredCreatePrincipal();

        if (hasAuthority(principal, "ROLE_COMPANY_ADMIN")) {
            return;
        }

        if (hasAuthority(principal, "ROLE_USER") && item.getCreatedByUser().getId().equals(principal.userId())) {
            return;
        }

        throw new AccessDeniedException("You can modify only your own items.");
    }

    private boolean hasAuthority(AresAuthenticatedPrincipal principal, String authority) {
        return principal.authorities().stream()
            .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}

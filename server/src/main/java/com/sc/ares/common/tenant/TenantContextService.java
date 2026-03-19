package com.sc.ares.common.tenant;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AresUserDetails;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantContextService {

    public TenantContext getRequiredTenantContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new InsufficientAuthenticationException("Authenticated tenant context is required.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AresUserDetails userDetails) {
            principal = userDetails.getPrincipal();
        }

        if (!(principal instanceof AresAuthenticatedPrincipal authenticatedPrincipal)) {
            throw new InsufficientAuthenticationException("Authenticated tenant context is required.");
        }

        return new TenantContext(
            authenticatedPrincipal.userId(),
            authenticatedPrincipal.companyId(),
            authenticatedPrincipal.platformAdmin()
        );
    }
}

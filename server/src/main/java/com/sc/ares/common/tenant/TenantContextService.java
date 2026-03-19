package com.sc.ares.common.tenant;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantContextService {

    public TenantContext getRequiredTenantContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AresAuthenticatedPrincipal principal)) {
            throw new InsufficientAuthenticationException("Authenticated tenant context is required.");
        }

        return new TenantContext(
            principal.userId(),
            principal.companyId(),
            principal.platformAdmin()
        );
    }
}

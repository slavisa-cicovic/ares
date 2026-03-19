package com.sc.ares.features.company.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.auth.service.CurrentPrincipalService;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CompanyAdminAccessService {

    private final CurrentPrincipalService currentPrincipalService;
    private final CompanyRepository companyRepository;

    public CompanyAdminAccessService(
        CurrentPrincipalService currentPrincipalService,
        CompanyRepository companyRepository
    ) {
        this.currentPrincipalService = currentPrincipalService;
        this.companyRepository = companyRepository;
    }

    public Company getRequiredManagedCompany() {
        AresAuthenticatedPrincipal principal = currentPrincipalService.getRequiredPrincipal();

        if (principal.platformAdmin() || !hasAuthority(principal, "ROLE_COMPANY_ADMIN")) {
            throw new AccessDeniedException("Company admin access is required.");
        }

        return companyRepository.findByIdAndDeletedAtIsNull(principal.companyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    public UUID getRequiredManagedCompanyId() {
        return getRequiredManagedCompany().getId();
    }

    public UUID getRequiredCurrentUserId() {
        return currentPrincipalService.getRequiredPrincipal().userId();
    }

    private boolean hasAuthority(AresAuthenticatedPrincipal principal, String authority) {
        return principal.authorities().stream()
            .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}

package com.sc.ares.features.company.service;

import com.sc.ares.features.company.dto.CompanyResponse;
import com.sc.ares.features.company.dto.UpdateCompanyRequest;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyManagementService {

    private final CompanyAdminAccessService companyAdminAccessService;
    private final CompanyRepository companyRepository;

    public CompanyManagementService(
        CompanyAdminAccessService companyAdminAccessService,
        CompanyRepository companyRepository
    ) {
        this.companyAdminAccessService = companyAdminAccessService;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCurrentCompany() {
        return toResponse(companyAdminAccessService.getRequiredManagedCompany());
    }

    @Transactional
    public CompanyResponse updateCurrentCompany(UpdateCompanyRequest request) {
        Company company = companyAdminAccessService.getRequiredManagedCompany();
        company.updateName(request.name().trim());
        companyRepository.save(company);

        return toResponse(company);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getStatus().name()
        );
    }
}

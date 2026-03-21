package com.sc.ares.features.company.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.features.company.dto.CompanyResponse;
import com.sc.ares.features.company.dto.UpdateCompanyRequest;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyThemePreset;
import com.sc.ares.features.company.repository.CompanyRepository;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyManagementService {

    private static final String SIMPLE_EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String HEX_COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";

    private final CompanyAdminAccessService companyAdminAccessService;
    private final CompanyRepository companyRepository;
    private final CompanyLogoStorageService companyLogoStorageService;

    public CompanyManagementService(
        CompanyAdminAccessService companyAdminAccessService,
        CompanyRepository companyRepository,
        CompanyLogoStorageService companyLogoStorageService
    ) {
        this.companyAdminAccessService = companyAdminAccessService;
        this.companyRepository = companyRepository;
        this.companyLogoStorageService = companyLogoStorageService;
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCurrentCompany() {
        return toResponse(companyAdminAccessService.getRequiredManagedCompany());
    }

    @Transactional
    public CompanyResponse updateCurrentCompany(UpdateCompanyRequest request) {
        Company company = companyAdminAccessService.getRequiredManagedCompany();
        company.updateProfile(
            normalizeRequired(request.name()),
            normalizeRequired(request.pib(), "PIB is required."),
            normalizeRequired(request.mb(), "MB is required."),
            normalizeOptional(request.address()),
            normalizeOptional(request.postalCode()),
            normalizeOptional(request.city()),
            normalizeOptional(request.country()),
            normalizeOptionalEmail(request.email()),
            normalizeOptional(request.phone()),
            normalizeOptional(request.bankAccount()),
            normalizeOptionalBrandColor(request.brandColor()),
            normalizeOptional(request.logoUrl()),
            normalizeThemePreset(request.themePreset())
        );
        ensureUniqueCompanyIdentifiers(company);
        companyRepository.save(company);

        return toResponse(company);
    }

    @Transactional(readOnly = true)
    public String storeCurrentCompanyLogo(MultipartFile file) {
        UUID companyId = companyAdminAccessService.getRequiredManagedCompany().getId();
        return companyLogoStorageService.storeLogo(companyId, file);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getPib(),
            company.getMb(),
            company.getAddress(),
            company.getPostalCode(),
            company.getCity(),
            company.getCountry(),
            company.getEmail(),
            company.getPhone(),
            company.getBankAccount(),
            company.getBrandColor(),
            company.getLogoUrl(),
            company.getThemePreset().toApiValue(),
            company.getStatus().name()
        );
    }

    private void ensureUniqueCompanyIdentifiers(Company company) {
        if (companyRepository.existsByPibAndDeletedAtIsNullAndIdNot(company.getPib(), company.getId())) {
            throw new ApiBadRequestException("PIB must be unique.");
        }

        if (companyRepository.existsByMbAndDeletedAtIsNullAndIdNot(company.getMb(), company.getId())) {
            throw new ApiBadRequestException("MB must be unique.");
        }
    }

    private String normalizeRequired(String value) {
        return normalizeRequired(value, "Required company field is missing.");
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new ApiBadRequestException(errorMessage);
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalEmail(String value) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        if (!normalized.matches(SIMPLE_EMAIL_PATTERN)) {
            throw new ApiBadRequestException("Invalid company email.");
        }

        return normalized;
    }

    private String normalizeOptionalBrandColor(String value) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        if (!normalized.matches(HEX_COLOR_PATTERN)) {
            throw new ApiBadRequestException("Invalid brand color.");
        }

        return normalized.toUpperCase();
    }

    private CompanyThemePreset normalizeThemePreset(String value) {
        try {
            return CompanyThemePreset.fromApiValue(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid company theme preset.");
        }
    }
}

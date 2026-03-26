package com.sc.ares.features.company.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.company.dto.CompanyResponse;
import com.sc.ares.features.company.dto.CompanySummaryResponse;
import com.sc.ares.features.company.dto.CreateCompanyRequest;
import com.sc.ares.features.company.dto.UpdateCompanyRequest;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.entity.CompanyThemePreset;
import com.sc.ares.features.company.repository.CompanyRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sc.ares.features.auth.service.PlatformAdminAccessService;

@Service
public class CompanyManagementService {

    private static final String SIMPLE_EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String HEX_COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyAdminAccessService companyAdminAccessService;
    private final PlatformAdminAccessService platformAdminAccessService;
    private final CompanyRepository companyRepository;
    private final CompanyLogoStorageService companyLogoStorageService;

    public CompanyManagementService(
        CompanyAdminAccessService companyAdminAccessService,
        PlatformAdminAccessService platformAdminAccessService,
        CompanyRepository companyRepository,
        CompanyLogoStorageService companyLogoStorageService
    ) {
        this.companyAdminAccessService = companyAdminAccessService;
        this.platformAdminAccessService = platformAdminAccessService;
        this.companyRepository = companyRepository;
        this.companyLogoStorageService = companyLogoStorageService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CompanySummaryResponse> listPlatformCompanies(
        int page,
        int size,
        String sort,
        String search
    ) {
        platformAdminAccessService.getRequiredPlatformAdminPrincipal();
        PageRequest pageRequest = PageRequest.of(
            normalizePage(page),
            normalizeSize(size),
            parsePlatformSort(sort)
        );
        String normalizedSearch = normalizeSearch(search);

        Page<Company> resultPage = companyRepository.findPageByPlatformFilters(normalizedSearch, pageRequest);

        return new PagedResponse<>(
            resultPage.getContent().stream().map(this::toSummaryResponse).toList(),
            resultPage.getNumber(),
            resultPage.getSize(),
            resultPage.getTotalElements(),
            resultPage.getTotalPages()
        );
    }

    @Transactional
    public CompanySummaryResponse createPlatformCompany(CreateCompanyRequest request) {
        platformAdminAccessService.getRequiredPlatformAdminPrincipal();

        String name = normalizeRequired(request.name(), "Company name is required.");
        String pib = normalizeRequired(request.pib(), "PIB is required.");
        String mb = normalizeRequired(request.mb(), "MB is required.");

        ensureUniqueCompanyIdentifiers(pib, mb);

        Company company = Company.create(name, CompanyStatus.ACTIVE);
        company.updateProfile(
            name,
            pib,
            mb,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            CompanyThemePreset.AMBER
        );

        return toSummaryResponse(companyRepository.save(company));
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

    private CompanySummaryResponse toSummaryResponse(Company company) {
        return new CompanySummaryResponse(
            company.getId(),
            company.getName(),
            company.getPib(),
            company.getMb(),
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

    private void ensureUniqueCompanyIdentifiers(String pib, String mb) {
        if (companyRepository.existsByPibAndDeletedAtIsNull(pib)) {
            throw new ApiBadRequestException("PIB must be unique.");
        }

        if (companyRepository.existsByMbAndDeletedAtIsNull(mb)) {
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

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Sort parsePlatformSort(String sort) {
        String requestedSort = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
        String[] parts = requestedSort.split(",", 2);
        String field = parts[0].trim();
        String mappedField = switch (field) {
            case "name" -> "name";
            case "pib" -> "pib";
            case "mb" -> "mb";
            case "status" -> "status";
            case "createdAt" -> "createdAt";
            default -> throw new ApiBadRequestException("Invalid sort field.");
        };

        Sort.Direction direction = Sort.Direction.DESC;

        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new ApiBadRequestException("Invalid sort direction.");
            }
        }

        return Sort.by(direction, mappedField);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }

        return search.trim();
    }
}

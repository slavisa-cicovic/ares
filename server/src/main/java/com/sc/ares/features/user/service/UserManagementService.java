package com.sc.ares.features.user.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.RoleScope;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.RoleRepository;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.auth.service.CurrentPrincipalService;
import com.sc.ares.features.auth.service.PlatformAdminAccessService;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.company.service.CompanyAdminAccessService;
import com.sc.ares.features.user.dto.ChangePasswordRequest;
import com.sc.ares.features.user.dto.CreatedUserResponse;
import com.sc.ares.features.user.dto.CreateUserRequest;
import com.sc.ares.features.user.dto.CreatePlatformUserRequest;
import com.sc.ares.features.user.dto.OwnProfileResponse;
import com.sc.ares.features.user.dto.PlatformCreatedUserResponse;
import com.sc.ares.features.user.dto.PlatformUserResponse;
import com.sc.ares.features.user.dto.UpdateOwnProfileRequest;
import com.sc.ares.features.user.dto.UpdateUserRequest;
import com.sc.ares.features.user.dto.UserResponse;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_ROLE_CODES = Set.of("company-admin", "user");
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "email", "email",
        "firstName", "firstName",
        "lastName", "lastName",
        "status", "status",
        "createdAt", "createdAt"
    );

    private final CompanyAdminAccessService companyAdminAccessService;
    private final PlatformAdminAccessService platformAdminAccessService;
    private final CurrentPrincipalService currentPrincipalService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLogoStorageService userLogoStorageService;

    public UserManagementService(
        CompanyAdminAccessService companyAdminAccessService,
        PlatformAdminAccessService platformAdminAccessService,
        CurrentPrincipalService currentPrincipalService,
        CompanyRepository companyRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        PasswordEncoder passwordEncoder,
        UserLogoStorageService userLogoStorageService
    ) {
        this.companyAdminAccessService = companyAdminAccessService;
        this.platformAdminAccessService = platformAdminAccessService;
        this.currentPrincipalService = currentPrincipalService;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userLogoStorageService = userLogoStorageService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlatformUserResponse> listPlatformUsers(
        int page,
        int size,
        String sort,
        String status,
        String search
    ) {
        platformAdminAccessService.getRequiredPlatformAdminPrincipal();
        PageRequest pageRequest = PageRequest.of(
            normalizePage(page),
            normalizeSize(size),
            parseSort(sort)
        );
        UserStatus userStatus = parseOptionalStatus(status);
        String normalizedSearch = normalizeSearch(search);

        Page<AppUser> resultPage = userRepository.findPageByPlatformFilters(
            userStatus,
            normalizedSearch,
            pageRequest
        );

        Map<UUID, List<String>> userRoles = loadRoleCodes(resultPage.getContent());

        return new PagedResponse<>(
            resultPage.getContent().stream()
                .map(user -> toPlatformResponse(user, userRoles.getOrDefault(user.getId(), List.of())))
                .toList(),
            resultPage.getNumber(),
            resultPage.getSize(),
            resultPage.getTotalElements(),
            resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(
        int page,
        int size,
        String sort,
        String status,
        String search
    ) {
        UUID companyId = companyAdminAccessService.getRequiredManagedCompanyId();
        PageRequest pageRequest = PageRequest.of(
            normalizePage(page),
            normalizeSize(size),
            parseSort(sort)
        );
        UserStatus userStatus = parseOptionalStatus(status);
        String normalizedSearch = normalizeSearch(search);

        Page<AppUser> resultPage = userRepository.findPageByCompanyIdAndFilters(
            companyId,
            userStatus,
            normalizedSearch,
            pageRequest
        );

        Map<UUID, List<String>> userRoles = loadRoleCodes(resultPage.getContent());

        return new PagedResponse<>(
            resultPage.getContent().stream()
                .map(user -> toResponse(user, userRoles.getOrDefault(user.getId(), List.of())))
                .toList(),
            resultPage.getNumber(),
            resultPage.getSize(),
            resultPage.getTotalElements(),
            resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        UUID companyId = companyAdminAccessService.getRequiredManagedCompanyId();
        AppUser user = getRequiredTenantUser(userId, companyId);
        List<String> roleCodes = loadRoleCodes(List.of(user)).getOrDefault(user.getId(), List.of());

        return toResponse(user, roleCodes);
    }

    @Transactional
    public CreatedUserResponse createUser(CreateUserRequest request) {
        Company company = companyAdminAccessService.getRequiredManagedCompany();
        ensureEmailAvailableForCreate(request.email());
        UserStatus status = parseRequiredStatus(request.status());
        List<Role> roles = resolveAssignableRoles(request.roles());
        String temporaryPassword = generateTemporaryPassword();

        AppUser user = AppUser.create(
            company,
            request.email().trim(),
            passwordEncoder.encode(temporaryPassword),
            request.firstName().trim(),
            request.lastName().trim(),
            status
        );

        try {
            AppUser savedUser = userRepository.save(user);
            replaceRoles(savedUser, roles);

            return new CreatedUserResponse(
                savedUser.getId(),
                company.getId(),
                company.getName(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getStatus().name(),
                roles.stream().map(Role::getCode).sorted().toList(),
                temporaryPassword
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessConflictException("User email already exists.");
        }
    }

    @Transactional
    public PlatformCreatedUserResponse createPlatformUser(CreatePlatformUserRequest request) {
        platformAdminAccessService.getRequiredPlatformAdminPrincipal();
        Company company = companyRepository.findByIdAndDeletedAtIsNull(request.companyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        ensureEmailAvailableForCreate(request.email());
        UserStatus status = parseRequiredStatus(request.status());
        List<Role> roles = resolveAssignableRoles(request.roles());
        String temporaryPassword = generateTemporaryPassword();

        AppUser user = AppUser.create(
            company,
            request.email().trim(),
            passwordEncoder.encode(temporaryPassword),
            request.firstName().trim(),
            request.lastName().trim(),
            status
        );

        try {
            AppUser savedUser = userRepository.save(user);
            replaceRoles(savedUser, roles);

            return new PlatformCreatedUserResponse(
                savedUser.getId(),
                company.getId(),
                company.getName(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getStatus().name(),
                roles.stream().map(Role::getCode).sorted().toList(),
                temporaryPassword
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessConflictException("User email already exists.");
        }
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        UUID companyId = companyAdminAccessService.getRequiredManagedCompanyId();
        UUID currentUserId = companyAdminAccessService.getRequiredCurrentUserId();
        AppUser user = getRequiredTenantUser(userId, companyId);
        UserStatus status = parseRequiredStatus(request.status());

        ensureEmailAvailableForUpdate(userId, request.email());

        if (currentUserId.equals(userId) && status != UserStatus.ACTIVE) {
            throw new BusinessConflictException("Company admin cannot deactivate their own account.");
        }

        List<Role> roles = resolveAssignableRoles(request.roles());

        user.updateIdentity(
            request.email().trim(),
            request.firstName().trim(),
            request.lastName().trim(),
            status
        );

        try {
            AppUser savedUser = userRepository.save(user);
            replaceRoles(savedUser, roles);

            return toResponse(savedUser, roles.stream().map(Role::getCode).sorted().toList());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessConflictException("User email already exists.");
        }
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UUID companyId = companyAdminAccessService.getRequiredManagedCompanyId();
        UUID currentUserId = companyAdminAccessService.getRequiredCurrentUserId();
        AppUser user = getRequiredTenantUser(userId, companyId);

        if (currentUserId.equals(userId)) {
            throw new BusinessConflictException("Company admin cannot delete their own account.");
        }

        user.softDelete();
        userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        AppUser user = userRepository.findByIdAndDeletedAtIsNull(currentPrincipalService.getRequiredPrincipal().userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiBadRequestException("Current password is incorrect.");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ApiBadRequestException("New password confirmation does not match.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiBadRequestException("New password must be different from the current password.");
        }

        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public OwnProfileResponse updateOwnProfile(UpdateOwnProfileRequest request) {
        UUID userId = currentPrincipalService.getRequiredPrincipal().userId();
        AppUser user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ensureEmailAvailableForUpdate(userId, request.email());

        user.updateOwnProfile(
            request.email().trim(),
            request.firstName().trim(),
            request.lastName().trim(),
            normalizeOptional(request.phone()),
            normalizeOptional(request.userLogoUrl())
        );

        AppUser savedUser = userRepository.save(user);
        List<String> roleCodes = loadRoleCodes(List.of(savedUser)).getOrDefault(savedUser.getId(), List.of());

        return new OwnProfileResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getPhone(),
            savedUser.getProfileImageUrl(),
            savedUser.getStatus().name(),
            roleCodes
        );
    }

    @Transactional
    public String storeOwnLogo(org.springframework.web.multipart.MultipartFile file) {
        AppUser user = userRepository.findByIdAndDeletedAtIsNull(currentPrincipalService.getRequiredPrincipal().userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return userLogoStorageService.storeLogo(user.getId(), file);
    }

    private AppUser getRequiredTenantUser(UUID userId, UUID companyId) {
        AppUser user = userRepository.findByIdAndCompanyIdAndDeletedAtIsNull(userId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        boolean hasPlatformRole = userRoleRepository.findAllByUserId(user.getId()).stream()
            .anyMatch(userRole -> userRole.getRole().getScope() == RoleScope.PLATFORM);

        if (hasPlatformRole) {
            throw new ResourceNotFoundException("User not found.");
        }

        return user;
    }

    private void ensureEmailAvailableForCreate(String email) {
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())) {
            throw new BusinessConflictException("User email already exists.");
        }
    }

    private void ensureEmailAvailableForUpdate(UUID userId, String email) {
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(email.trim(), userId)) {
            throw new BusinessConflictException("User email already exists.");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<Role> resolveAssignableRoles(List<String> requestedRoleCodes) {
        LinkedHashSet<String> normalizedRoleCodes = requestedRoleCodes.stream()
            .map(String::trim)
            .filter(roleCode -> !roleCode.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedRoleCodes.isEmpty()) {
            throw new ApiBadRequestException("At least one role is required.");
        }

        if (!ALLOWED_ROLE_CODES.containsAll(normalizedRoleCodes)) {
            throw new ApiBadRequestException("Only tenant roles company-admin and user may be assigned.");
        }

        List<Role> roles = roleRepository.findAllByCompanyIsNullAndScopeAndCodeIn(
            RoleScope.COMPANY,
            normalizedRoleCodes
        );

        if (roles.size() != normalizedRoleCodes.size()) {
            throw new ApiBadRequestException("One or more requested roles are invalid.");
        }

        return roles.stream()
            .sorted(Comparator.comparing(Role::getCode))
            .toList();
    }

    private void replaceRoles(AppUser user, List<Role> roles) {
        userRoleRepository.deleteByUserId(user.getId());

        List<UserRole> userRoles = new ArrayList<>();

        for (Role role : roles) {
            userRoles.add(UserRole.create(user, role));
        }

        userRoleRepository.saveAll(userRoles);
    }

    private Map<UUID, List<String>> loadRoleCodes(List<AppUser> users) {
        if (users.isEmpty()) {
            return Map.of();
        }

        List<UUID> userIds = users.stream()
            .map(AppUser::getId)
            .toList();

        return userRoleRepository.findAllByUserIdIn(userIds).stream()
            .collect(Collectors.groupingBy(
                userRole -> userRole.getUser().getId(),
                Collectors.mapping(userRole -> userRole.getRole().getCode(), Collectors.collectingAndThen(
                    Collectors.toList(),
                    roleCodes -> roleCodes.stream().sorted().toList()
                ))
            ));
    }

    private UserResponse toResponse(AppUser user, List<String> roleCodes) {
        return new UserResponse(
            user.getId(),
            user.getCompany().getId(),
            user.getCompany().getName(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            roleCodes
        );
    }

    private PlatformUserResponse toPlatformResponse(AppUser user, List<String> roleCodes) {
        return new PlatformUserResponse(
            user.getId(),
            user.getCompany().getId(),
            user.getCompany().getName(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            roleCodes
        );
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

    private Sort parseSort(String sort) {
        String requestedSort = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
        String[] parts = requestedSort.split(",", 2);
        String field = parts[0].trim();
        String mappedField = SORT_FIELD_MAPPING.get(field);

        if (mappedField == null) {
            throw new ApiBadRequestException("Invalid sort field.");
        }

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

    private UserStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return parseRequiredStatus(status);
    }

    private UserStatus parseRequiredStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid user status.");
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }

        return search.trim();
    }

    private String generateTemporaryPassword() {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < 12; index++) {
            builder.append(TEMP_PASSWORD_ALPHABET.charAt(random.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }

        return builder.toString();
    }
}

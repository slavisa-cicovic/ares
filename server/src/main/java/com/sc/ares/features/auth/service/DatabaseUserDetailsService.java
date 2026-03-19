package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AresUserDetails;
import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.RoleScope;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DatabaseUserDetailsService(
        UserRepository userRepository,
        UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        List<UserRole> userRoles = userRoleRepository.findAllByUserId(user.getId());
        List<String> roleCodes = userRoles.stream()
            .map(UserRole::getRole)
            .map(Role::getCode)
            .sorted()
            .toList();

        AresAuthenticatedPrincipal principal = new AresAuthenticatedPrincipal(
            user.getId(),
            user.getCompany().getId(),
            user.getEmail(),
            userRoles.stream().anyMatch(userRole -> isPlatformSuperAdmin(userRole.getRole())),
            roleCodes.stream()
                .map(this::toAuthorityCode)
                .map(SimpleGrantedAuthority::new)
                .toList()
        );

        return new AresUserDetails(
            principal,
            user.getPasswordHash(),
            user.getFirstName(),
            user.getLastName(),
            user.getCompany().getName(),
            roleCodes,
            isLoginEnabled(user, user.getCompany())
        );
    }

    @Transactional(readOnly = true)
    public AresUserDetails loadCurrentUserById(java.util.UUID userId) {
        AppUser user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        return (AresUserDetails) loadUserByUsername(user.getEmail());
    }

    @Transactional(readOnly = true)
    public AppUser getRequiredUserEntity(java.util.UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    private boolean isLoginEnabled(AppUser user, Company company) {
        return user.getDeletedAt() == null
            && user.getStatus() == UserStatus.ACTIVE
            && company.getDeletedAt() == null
            && company.getStatus() == CompanyStatus.ACTIVE;
    }

    private boolean isPlatformSuperAdmin(Role role) {
        return role.getScope() == RoleScope.PLATFORM && "super-admin".equals(role.getCode());
    }

    private String toAuthorityCode(String roleCode) {
        return "ROLE_" + roleCode.toUpperCase().replace('-', '_');
    }
}

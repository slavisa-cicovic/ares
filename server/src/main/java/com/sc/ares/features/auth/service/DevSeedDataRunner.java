package com.sc.ares.features.auth.service;

import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.RoleRepository;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevSeedDataRunner implements ApplicationRunner {

    private static final String DEV_PASSWORD = "ares-dev";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedDataRunner(
        CompanyRepository companyRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Company companyOne = ensureCompany("Ares Demo One");
        Company companyTwo = ensureCompany("Ares Demo Two");

        Map<String, Role> roles = Map.of(
            "super-admin", getRequiredRole("super-admin"),
            "company-admin", getRequiredRole("company-admin"),
            "user", getRequiredRole("user")
        );

        ensureUserWithRole(
            companyOne,
            "superadmin@local.ares",
            "Super",
            "Admin",
            roles.get("super-admin")
        );
        ensureUserWithRole(
            companyOne,
            "admin.one@local.ares",
            "Admin",
            "One",
            roles.get("company-admin")
        );
        ensureUserWithRole(
            companyOne,
            "user.one@local.ares",
            "User",
            "One",
            roles.get("user")
        );
        ensureUserWithRole(
            companyTwo,
            "admin.two@local.ares",
            "Admin",
            "Two",
            roles.get("company-admin")
        );
        ensureUserWithRole(
            companyTwo,
            "user.two@local.ares",
            "User",
            "Two",
            roles.get("user")
        );
    }

    private Company ensureCompany(String name) {
        return companyRepository.findByNameAndDeletedAtIsNull(name)
            .orElseGet(() -> companyRepository.save(Company.create(name, CompanyStatus.ACTIVE)));
    }

    private AppUser ensureUserWithRole(
        Company company,
        String email,
        String firstName,
        String lastName,
        Role role
    ) {
        AppUser user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
            .orElseGet(() -> userRepository.save(
                AppUser.create(
                    company,
                    email,
                    passwordEncoder.encode(DEV_PASSWORD),
                    firstName,
                    lastName,
                    UserStatus.ACTIVE
                )
            ));

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(UserRole.create(user, role));
        }

        return user;
    }

    private Role getRequiredRole(String code) {
        return roleRepository.findByCode(code)
            .orElseThrow(() -> new IllegalStateException("Required role not found: " + code));
    }
}

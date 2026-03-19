package com.sc.ares;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.RoleScope;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AuthLoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company activeCompany;
    private Role superAdminRole;
    private Role companyAdminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        companyRepository.deleteAll();

        activeCompany = companyRepository.save(Company.create("Test Company", CompanyStatus.ACTIVE));
        Company secondaryCompany = companyRepository.save(Company.create("Secondary Company", CompanyStatus.ACTIVE));

        superAdminRole = roleRepository.save(Role.create(
            null,
            "super-admin",
            "Super Admin",
            RoleScope.PLATFORM,
            true
        ));
        companyAdminRole = roleRepository.save(Role.create(
            null,
            "company-admin",
            "Company Admin",
            RoleScope.COMPANY,
            true
        ));
        userRole = roleRepository.save(Role.create(
            null,
            "user",
            "User",
            RoleScope.COMPANY,
            true
        ));

        createUser(activeCompany, "active@local.ares", UserStatus.ACTIVE, false, userRole);
        createUser(activeCompany, "admin@local.ares", UserStatus.ACTIVE, false, companyAdminRole);
        createUser(activeCompany, "superadmin@local.ares", UserStatus.ACTIVE, false, superAdminRole);
        createUser(activeCompany, "inactive@local.ares", UserStatus.INACTIVE, false, userRole);
        createUser(activeCompany, "suspended@local.ares", UserStatus.SUSPENDED, false, userRole);
        createUser(activeCompany, "deleted@local.ares", UserStatus.ACTIVE, true, userRole);

        Company inactiveCompany = companyRepository.save(Company.create("Inactive Company", CompanyStatus.INACTIVE));
        createUser(inactiveCompany, "inactive-company@local.ares", UserStatus.ACTIVE, false, userRole);

        Company deletedCompany = companyRepository.save(Company.create("Deleted Company", CompanyStatus.ACTIVE));
        deletedCompany.softDelete();
        companyRepository.save(deletedCompany);
        createUser(deletedCompany, "deleted-company@local.ares", UserStatus.ACTIVE, false, userRole);

        createUser(secondaryCompany, "user.two@local.ares", UserStatus.ACTIVE, false, userRole);
    }

    @Test
    void loginShouldReturnAuthenticatedUserPayload() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest("admin@local.ares", "ares-test"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@local.ares"))
            .andExpect(jsonPath("$.companyName").value("Test Company"))
            .andExpect(jsonPath("$.roles[0]").value("company-admin"))
            .andExpect(jsonPath("$.platformAdmin").value(false))
            .andExpect(jsonPath("$.authenticationType").value("BASIC"));
    }

    @Test
    void loginShouldRejectUnknownEmail() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest("missing@local.ares", "ares-test"))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void loginShouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest("active@local.ares", "wrong-password"))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void loginShouldRejectInactiveUser() throws Exception {
        assertLoginRejected("inactive@local.ares");
    }

    @Test
    void loginShouldRejectSuspendedUser() throws Exception {
        assertLoginRejected("suspended@local.ares");
    }

    @Test
    void loginShouldRejectDeletedUser() throws Exception {
        assertLoginRejected("deleted@local.ares");
    }

    @Test
    void loginShouldRejectUserFromInactiveCompany() throws Exception {
        assertLoginRejected("inactive-company@local.ares");
    }

    @Test
    void loginShouldRejectUserFromDeletedCompany() throws Exception {
        assertLoginRejected("deleted-company@local.ares");
    }

    @Test
    void currentUserEndpointShouldReturnIdentityForAuthenticatedUser() throws Exception {
        mockMvc.perform(
                get("/auth/me")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic("superadmin@local.ares", "ares-test"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("superadmin@local.ares"))
            .andExpect(jsonPath("$.roles[0]").value("super-admin"))
            .andExpect(jsonPath("$.platformAdmin").value(true))
            .andExpect(jsonPath("$.companyId").value(activeCompany.getId().toString()));
    }

    private void assertLoginRejected(String email) throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest(email, "ares-test"))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    private String loginRequest(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "email", email,
            "password", password
        ));
    }

    private AppUser createUser(
        Company company,
        String email,
        UserStatus status,
        boolean deleted,
        Role role
    ) {
        AppUser user = AppUser.create(
            company,
            email,
            passwordEncoder.encode("ares-test"),
            "Test",
            "User",
            status
        );

        if (deleted) {
            user.softDelete();
        }

        AppUser savedUser = userRepository.save(user);
        userRoleRepository.save(UserRole.create(savedUser, role));

        return savedUser;
    }
}

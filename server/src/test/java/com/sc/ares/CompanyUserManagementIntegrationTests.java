package com.sc.ares;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.RoleScope;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.RefreshTokenRepository;
import com.sc.ares.features.auth.repository.RoleRepository;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CompanyUserManagementIntegrationTests {

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company companyOne;
    private Company companyTwo;
    private Role companyAdminRole;
    private Role userRole;
    private AppUser adminOne;
    private AppUser userOne;
    private AppUser adminTwo;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        companyRepository.deleteAll();

        companyOne = companyRepository.save(Company.create("Alpha Company", CompanyStatus.ACTIVE));
        companyTwo = companyRepository.save(Company.create("Beta Company", CompanyStatus.ACTIVE));

        roleRepository.save(Role.create(null, "super-admin", "Super Admin", RoleScope.PLATFORM, true));
        companyAdminRole = roleRepository.save(Role.create(null, "company-admin", "Company Admin", RoleScope.COMPANY, true));
        userRole = roleRepository.save(Role.create(null, "user", "User", RoleScope.COMPANY, true));

        adminOne = createUser(companyOne, "admin.one@local.ares", UserStatus.ACTIVE, companyAdminRole);
        userOne = createUser(companyOne, "user.one@local.ares", UserStatus.ACTIVE, userRole);
        adminTwo = createUser(companyTwo, "admin.two@local.ares", UserStatus.ACTIVE, companyAdminRole);
        createUser(companyTwo, "user.two@local.ares", UserStatus.ACTIVE, userRole);
    }

    @Test
    void companyAdminShouldGetAndUpdateOwnCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/company")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alpha Company"));

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("name", "Alpha Company Updated")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alpha Company Updated"));
    }

    @Test
    void regularUserShouldBeRejectedFromCompanyAdminRoutes() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/company")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void companyAdminShouldListOnlyOwnCompanyUsers() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/users")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[*].email").value(org.hamcrest.Matchers.containsInAnyOrder(
                "admin.one@local.ares",
                "user.one@local.ares"
            )));
    }

    @Test
    void companyAdminShouldCreateUserInOwnCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/users")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "new.user@local.ares",
                        "password", "secret123",
                        "firstName", "New",
                        "lastName", "User",
                        "status", "ACTIVE",
                        "roles", List.of("user")
                    )))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new.user@local.ares"))
            .andExpect(jsonPath("$.roles[0]").value("user"));
    }

    @Test
    void companyAdminShouldRejectSuperAdminRoleAssignment() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/users")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "forbidden.role@local.ares",
                        "password", "secret123",
                        "firstName", "Forbidden",
                        "lastName", "Role",
                        "status", "ACTIVE",
                        "roles", List.of("super-admin")
                    )))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only tenant roles company-admin and user may be assigned."));
    }

    @Test
    void companyAdminShouldNotUpdateAnotherCompanyUser() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/users/{userId}", adminTwo.getId())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "admin.two@local.ares",
                        "firstName", "Admin",
                        "lastName", "Two",
                        "status", "ACTIVE",
                        "roles", List.of("company-admin")
                    )))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void companyAdminShouldSoftDeleteOwnCompanyUser() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/users/{userId}", userOne.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/users/{userId}", userOne.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void companyAdminShouldRejectDuplicateEmail() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/users")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "user.two@local.ares",
                        "password", "secret123",
                        "firstName", "Dupe",
                        "lastName", "User",
                        "status", "ACTIVE",
                        "roles", List.of("user")
                    )))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("User email already exists."));
    }

    @Test
    void companyAdminShouldNotDeleteOwnAccount() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/users/{userId}", adminOne.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Company admin cannot delete their own account."));
    }

    private AppUser createUser(Company company, String email, UserStatus status, Role role) {
        AppUser user = AppUser.create(
            company,
            email,
            passwordEncoder.encode("ares-test"),
            "Test",
            "User",
            status
        );
        AppUser savedUser = userRepository.save(user);
        userRoleRepository.save(UserRole.create(savedUser, role));
        return savedUser;
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password
                    )))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

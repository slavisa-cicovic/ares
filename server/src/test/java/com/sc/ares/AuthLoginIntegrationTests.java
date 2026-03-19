package com.sc.ares;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.ares.features.auth.entity.RefreshToken;
import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.RoleScope;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.RefreshTokenRepository;
import com.sc.ares.features.auth.repository.RoleRepository;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.item.repository.ItemRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company activeCompany;
    private Role superAdminRole;
    private Role companyAdminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRoleRepository.deleteAll();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        itemRepository.deleteAll();
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
    void loginShouldReturnTokenPayload() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest("admin@local.ares", "ares-test"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.user.email").value("admin@local.ares"))
            .andExpect(jsonPath("$.user.companyName").value("Test Company"))
            .andExpect(jsonPath("$.user.roles[0]").value("company-admin"))
            .andExpect(jsonPath("$.user.platformAdmin").value(false));
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
        JsonNode loginResponse = login("superadmin@local.ares", "ares-test");

        mockMvc.perform(
                get("/auth/me")
                    .header("Authorization", "Bearer " + loginResponse.get("accessToken").asText())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("superadmin@local.ares"))
            .andExpect(jsonPath("$.roles[0]").value("super-admin"))
            .andExpect(jsonPath("$.platformAdmin").value(true))
            .andExpect(jsonPath("$.companyId").value(activeCompany.getId().toString()));
    }

    @Test
    void currentUserEndpointShouldRejectInvalidBearerToken() throws Exception {
        mockMvc.perform(
                get("/auth/me")
                    .header("Authorization", "Bearer invalid-token")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid access token."));
    }

    @Test
    void refreshShouldRotateTokens() throws Exception {
        JsonNode loginResponse = login("active@local.ares", "ares-test");
        String originalRefreshToken = loginResponse.get("refreshToken").asText();

        mockMvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(originalRefreshToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(originalRefreshToken)))
            .andExpect(jsonPath("$.user.email").value("active@local.ares"));
    }

    @Test
    void refreshShouldRejectRevokedRefreshToken() throws Exception {
        JsonNode loginResponse = login("active@local.ares", "ares-test");
        String refreshToken = loginResponse.get("refreshToken").asText();

        mockMvc.perform(
                post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(refreshToken))
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(refreshToken))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void refreshShouldRejectExpiredRefreshToken() throws Exception {
        AppUser user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("active@local.ares")
            .orElseThrow();
        String rawExpiredToken = "expired-refresh-token";
        RefreshToken expiredToken = RefreshToken.create(user, sha256(rawExpiredToken), java.time.Instant.now().minusSeconds(60));
        refreshTokenRepository.save(expiredToken);

        mockMvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(rawExpiredToken))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void refreshShouldRejectWhenUserBecomesInactive() throws Exception {
        JsonNode loginResponse = login("active@local.ares", "ares-test");
        String refreshToken = loginResponse.get("refreshToken").asText();
        AppUser user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("active@local.ares")
            .orElseThrow();
        user.updateStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        mockMvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(refreshToken))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void refreshShouldRejectWhenCompanyBecomesInactive() throws Exception {
        JsonNode loginResponse = login("active@local.ares", "ares-test");
        String refreshToken = loginResponse.get("refreshToken").asText();
        Company company = companyRepository.findById(activeCompany.getId()).orElseThrow();
        company.updateStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);

        mockMvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest(refreshToken))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid refresh token."));
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

    private String refreshRequest(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "refreshToken", refreshToken
        ));
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest(email, password))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

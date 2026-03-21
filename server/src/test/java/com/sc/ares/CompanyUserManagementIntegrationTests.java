package com.sc.ares;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.item.repository.ItemRepository;
import com.sc.ares.features.payment.repository.PaymentRepository;
import com.sc.ares.features.refund.repository.CreditNoteRefundRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ares.storage.base-dir=/tmp/ares-company-logo-tests"
})
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
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreditNoteRefundRepository creditNoteRefundRepository;

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
        creditNoteRefundRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        itemRepository.deleteAll();
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
            .andExpect(jsonPath("$.name").value("Alpha Company"))
            .andExpect(jsonPath("$.pib").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.bankAccount").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.postalCode").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.city").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.country").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.brandColor").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.logoUrl").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.themePreset").value("amber"));

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.ofEntries(
                        Map.entry("name", "Alpha Company Updated"),
                        Map.entry("pib", "123456789"),
                        Map.entry("mb", "98765432"),
                        Map.entry("address", "Main Street 1"),
                        Map.entry("postalCode", "11000"),
                        Map.entry("city", "Belgrade"),
                        Map.entry("country", "Serbia"),
                        Map.entry("email", "office@alpha.local"),
                        Map.entry("phone", "+38160123456"),
                        Map.entry("bankAccount", "160-123456-78"),
                        Map.entry("brandColor", "#4B8AB4"),
                        Map.entry("logoUrl", "https://cdn.local/alpha.png"),
                        Map.entry("themePreset", "ocean")
                    )))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alpha Company Updated"))
            .andExpect(jsonPath("$.pib").value("123456789"))
            .andExpect(jsonPath("$.mb").value("98765432"))
            .andExpect(jsonPath("$.address").value("Main Street 1"))
            .andExpect(jsonPath("$.postalCode").value("11000"))
            .andExpect(jsonPath("$.city").value("Belgrade"))
            .andExpect(jsonPath("$.country").value("Serbia"))
            .andExpect(jsonPath("$.email").value("office@alpha.local"))
            .andExpect(jsonPath("$.phone").value("+38160123456"))
            .andExpect(jsonPath("$.bankAccount").value("160-123456-78"))
            .andExpect(jsonPath("$.brandColor").value("#4B8AB4"))
            .andExpect(jsonPath("$.logoUrl").value("https://cdn.local/alpha.png"))
            .andExpect(jsonPath("$.themePreset").value("ocean"));
    }

    @Test
    void companyAdminShouldNormalizeOptionalCompanyFields() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.ofEntries(
                        Map.entry("name", "  Alpha Company Updated  "),
                        Map.entry("pib", "  123456789  "),
                        Map.entry("mb", "  98765432  "),
                        Map.entry("address", "  Address  "),
                        Map.entry("postalCode", "  11000  "),
                        Map.entry("city", "  Belgrade  "),
                        Map.entry("country", "  Serbia  "),
                        Map.entry("email", "  office@alpha.local  "),
                        Map.entry("phone", "  +38160123456  "),
                        Map.entry("bankAccount", "   "),
                        Map.entry("brandColor", "  #B7844F  "),
                        Map.entry("logoUrl", "  https://cdn.local/logo.png  "),
                        Map.entry("themePreset", "graphite")
                    )))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alpha Company Updated"))
            .andExpect(jsonPath("$.pib").value("123456789"))
            .andExpect(jsonPath("$.mb").value("98765432"))
            .andExpect(jsonPath("$.address").value("Address"))
            .andExpect(jsonPath("$.postalCode").value("11000"))
            .andExpect(jsonPath("$.city").value("Belgrade"))
            .andExpect(jsonPath("$.country").value("Serbia"))
            .andExpect(jsonPath("$.email").value("office@alpha.local"))
            .andExpect(jsonPath("$.phone").value("+38160123456"))
            .andExpect(jsonPath("$.bankAccount").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.brandColor").value("#B7844F"))
            .andExpect(jsonPath("$.logoUrl").value("https://cdn.local/logo.png"))
            .andExpect(jsonPath("$.themePreset").value("graphite"));

    }

    @Test
    void companyAdminShouldUploadCompanyLogo() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "alpha-logo.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 }
        );

        MvcResult uploadResult = mockMvc.perform(
                multipart("/company/logo")
                    .file(file)
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logoUrl").value(Matchers.containsString("/api/company/logo/")))
            .andReturn();

        String logoUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
            .get("logoUrl")
            .asText();
        String logoPath = URI.create(logoUrl).getPath().replaceFirst("^/api", "");

        mockMvc.perform(get(logoPath))
            .andExpect(status().isOk())
            .andExpect(result -> org.hamcrest.MatcherAssert.assertThat(
                result.getResponse().getContentType(),
                Matchers.startsWith(MediaType.IMAGE_PNG_VALUE)
            ));
    }

    @Test
    void companyAdminShouldRejectInvalidBrandColor() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alpha Company Updated",
                        "pib", "123456789",
                        "mb", "98765432",
                        "brandColor", "blue"
                    )))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid brand color."));
    }

    @Test
    void companyAdminShouldRejectMissingPibAndMb() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alpha Company Updated",
                        "pib", "   ",
                        "mb", ""
                    )))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("PIB is required."));
    }

    @Test
    void companyAdminShouldRejectDuplicatePibAndMb() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        companyTwo.updateProfile(
            "Beta Company",
            "123456789",
            "98765432",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            com.sc.ares.features.company.entity.CompanyThemePreset.AMBER
        );
        companyRepository.save(companyTwo);

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alpha Company Updated",
                        "pib", "123456789",
                        "mb", "55555555"
                    )))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("PIB must be unique."));

        mockMvc.perform(
                put("/company")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alpha Company Updated",
                        "pib", "111111111",
                        "mb", "98765432"
                    )))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("MB must be unique."));
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
    void authenticatedUserShouldUpdateOwnProfile() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/users/me")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "user.one.updated@local.ares",
                        "firstName", "Mila",
                        "lastName", "Nikolic",
                        "phone", "+38160111222",
                        "userLogoUrl", "https://cdn.local/user-one.png"
                    )))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user.one.updated@local.ares"))
            .andExpect(jsonPath("$.firstName").value("Mila"))
            .andExpect(jsonPath("$.lastName").value("Nikolic"))
            .andExpect(jsonPath("$.phone").value("+38160111222"))
            .andExpect(jsonPath("$.userLogoUrl").value("https://cdn.local/user-one.png"))
            .andExpect(jsonPath("$.roles[0]").value("user"));
    }

    @Test
    void authenticatedUserShouldUploadOwnLogo() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "user-logo.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 }
        );

        MvcResult uploadResult = mockMvc.perform(
                multipart("/users/me/logo")
                    .file(file)
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logoUrl").value(Matchers.containsString("/api/users/logo/")))
            .andReturn();

        String logoUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
            .get("logoUrl")
            .asText();
        String logoPath = URI.create(logoUrl).getPath().replaceFirst("^/api", "");

        mockMvc.perform(get(logoPath))
            .andExpect(status().isOk())
            .andExpect(result -> org.hamcrest.MatcherAssert.assertThat(
                result.getResponse().getContentType(),
                Matchers.startsWith(MediaType.IMAGE_PNG_VALUE)
            ));
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

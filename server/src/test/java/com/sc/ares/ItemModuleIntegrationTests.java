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
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.item.entity.Item;
import com.sc.ares.features.item.entity.ItemStatus;
import com.sc.ares.features.item.repository.ItemRepository;
import com.sc.ares.features.payment.repository.PaymentRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.math.BigDecimal;
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
class ItemModuleIntegrationTests {

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
    private PasswordEncoder passwordEncoder;

    private Company companyOne;
    private Company companyTwo;
    private Item companyOneItem;
    private Item companyTwoItem;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRoleRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        companyRepository.deleteAll();

        companyOne = companyRepository.save(Company.create("Alpha Company", CompanyStatus.ACTIVE));
        companyTwo = companyRepository.save(Company.create("Beta Company", CompanyStatus.ACTIVE));

        Role companyAdminRole = roleRepository.save(Role.create(
            null, "company-admin", "Company Admin", RoleScope.COMPANY, true
        ));
        Role userRole = roleRepository.save(Role.create(
            null, "user", "User", RoleScope.COMPANY, true
        ));

        createUser(companyOne, "admin.one@local.ares", companyAdminRole);
        createUser(companyOne, "user.one@local.ares", userRole);
        createUser(companyTwo, "admin.two@local.ares", companyAdminRole);

        companyOneItem = itemRepository.save(Item.create(
            companyOne,
            "Development Service",
            "DEV-001",
            "Monthly development service",
            "hour",
            new BigDecimal("120.00"),
            new BigDecimal("20.00"),
            ItemStatus.ACTIVE
        ));
        companyTwoItem = itemRepository.save(Item.create(
            companyTwo,
            "Beta Support",
            "SUP-001",
            "Support package",
            "service",
            new BigDecimal("90.00"),
            new BigDecimal("10.00"),
            ItemStatus.ACTIVE
        ));
    }

    @Test
    void companyAdminShouldCreateItemInOwnCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/items")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Consulting", "CONS-001", "hour", "200.00", "20.00", "ACTIVE"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Consulting"))
            .andExpect(jsonPath("$.code").value("CONS-001"))
            .andExpect(jsonPath("$.price").value(200.00))
            .andExpect(jsonPath("$.taxRate").value(20.00));
    }

    @Test
    void regularUserShouldReadOwnCompanyItems() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/items")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Development Service"));
    }

    @Test
    void regularUserShouldNotCreateItem() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/items")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Nope", "NO-001", "piece", "10.00", "20.00", "ACTIVE"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void itemListShouldReturnOnlyOwnCompanyItems() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/items")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Development Service"));
    }

    @Test
    void crossTenantDetailShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/items/{itemId}", companyTwoItem.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantUpdateShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/items/{itemId}", companyTwoItem.getId())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Updated", "SUP-001", "service", "100.00", "20.00", "ACTIVE"))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantDeleteShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/items/{itemId}", companyTwoItem.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void companyAdminShouldUpdateOwnItem() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/items/{itemId}", companyOneItem.getId())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Development Retainer", "DEV-001", "month", "1500.00", "20.00", "INACTIVE"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Development Retainer"))
            .andExpect(jsonPath("$.status").value("INACTIVE"))
            .andExpect(jsonPath("$.unit").value("month"));
    }

    @Test
    void softDeletedItemShouldDisappearFromNormalReads() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/items/{itemId}", companyOneItem.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/items/{itemId}", companyOneItem.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidSortFieldShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/items")
                    .header("Authorization", bearer(token))
                    .param("sort", "unknown,asc")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid sort field."));
    }

    @Test
    void invalidPayloadShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/items")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "",
                        "unit", "",
                        "price", "-1.00",
                        "taxRate", "20.00",
                        "status", "ACTIVE"
                    )))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateCodeWithinSameCompanyShouldReturnConflict() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/items")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Duplicate", "DEV-001", "hour", "50.00", "20.00", "ACTIVE"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Item code already exists."));
    }

    @Test
    void sameCodeInAnotherCompanyShouldBeAllowed() throws Exception {
        String token = login("admin.two@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/items")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(itemPayload("Another Support", "DEV-001", "service", "75.00", "20.00", "ACTIVE"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("DEV-001"));
    }

    private AppUser createUser(Company company, String email, Role role) {
        AppUser user = AppUser.create(
            company,
            email,
            passwordEncoder.encode("ares-test"),
            "Test",
            "User",
            UserStatus.ACTIVE
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

    private String itemPayload(
        String name,
        String code,
        String unit,
        String price,
        String taxRate,
        String status
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "name", name,
            "code", code,
            "description", "Item description",
            "unit", unit,
            "price", price,
            "taxRate", taxRate,
            "status", status
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

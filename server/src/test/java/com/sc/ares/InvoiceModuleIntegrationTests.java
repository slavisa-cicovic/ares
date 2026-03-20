package com.sc.ares;

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
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.entity.ClientStatus;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class InvoiceModuleIntegrationTests {

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
    private ItemRepository itemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company companyOne;
    private Company companyTwo;
    private Client companyOneClient;
    private Client companyTwoClient;
    private Item companyOneItem;
    private Item companyTwoItem;
    private AppUser adminOne;
    private AppUser adminTwo;

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

        adminOne = createUser(companyOne, "admin.one@local.ares", companyAdminRole);
        createUser(companyOne, "user.one@local.ares", userRole);
        adminTwo = createUser(companyTwo, "admin.two@local.ares", companyAdminRole);

        companyOneClient = clientRepository.save(Client.create(
            companyOne, "Acme Client", "111", "222", "acme@local.ares", "123", "Street 1", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE
        ));
        companyTwoClient = clientRepository.save(Client.create(
            companyTwo, "Beta Client", "333", "444", "beta@local.ares", "456", "Neka 2", "Novi Sad", "21000", "Serbia", ClientStatus.ACTIVE
        ));
        companyOneItem = itemRepository.save(Item.create(
            companyOne, adminOne, "Development Service", "DEV-001", "Monthly development", "hour",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("1.000"), ItemStatus.ACTIVE
        ));
        companyTwoItem = itemRepository.save(Item.create(
            companyTwo, adminTwo, "Support Service", "SUP-001", "Support", "hour",
            new BigDecimal("80.00"), new BigDecimal("10.00"), new BigDecimal("1.000"), ItemStatus.ACTIVE
        ));
    }

    @Test
    void companyAdminShouldCreateDraftInvoiceInOwnCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyOneItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").value("2026-0001"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.clientName").value("Acme Client"))
            .andExpect(jsonPath("$.subtotal").value(400.00))
            .andExpect(jsonPath("$.taxTotal").value(80.00))
            .andExpect(jsonPath("$.total").value(480.00))
            .andExpect(jsonPath("$.lines.length()").value(1))
            .andExpect(jsonPath("$.lines[0].itemName").value("Development Service"));
    }

    @Test
    void regularUserShouldReadInvoiceList() throws Exception {
        String adminToken = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String userToken = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyOneItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(userToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("2026-0001"));
    }

    @Test
    void regularUserShouldNotCreateInvoice() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyOneItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void invoiceListShouldReturnOnlyOwnCompanyInvoices() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(tokenOne))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyOneItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(tokenTwo))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyTwoClient.getId(), companyTwoItem.getId(), "2026-03-20", "2026-03-27"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(tokenOne))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].clientName").value("Acme Client"));
    }

    @Test
    void crossTenantDetailShouldReturnNotFound() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();

        JsonNode createdInvoice = createInvoice(tokenTwo, companyTwoClient.getId(), companyTwoItem.getId());

        mockMvc.perform(
                get("/invoices/{invoiceId}", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantUpdateShouldReturnNotFound() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();

        JsonNode createdInvoice = createInvoice(tokenTwo, companyTwoClient.getId(), companyTwoItem.getId());

        mockMvc.perform(
                put("/invoices/{invoiceId}", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyOneItem.getId(), "2026-03-21", "2026-03-30"))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void createShouldRejectClientFromAnotherCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyTwoClient.getId(), companyOneItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Client not found."));
    }

    @Test
    void createShouldRejectItemFromAnotherCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(companyOneClient.getId(), companyTwoItem.getId(), "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Item not found."));
    }

    @Test
    void updateShouldReplaceDraftLinesAndRecalculateTotals() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                put("/invoices/{invoiceId}", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedInvoicePayload(companyOneClient.getId(), companyOneItem.getId()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal").value(300.00))
            .andExpect(jsonPath("$.taxTotal").value(60.00))
            .andExpect(jsonPath("$.total").value(360.00))
            .andExpect(jsonPath("$.lines.length()").value(2))
            .andExpect(jsonPath("$.lines[1].itemName").value("Additional Review"));
    }

    @Test
    void companyAdminShouldIssueOwnDraftInvoice() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"));

        UUID invoiceId = UUID.fromString(createdInvoice.get("id").asText());
        org.assertj.core.api.Assertions.assertThat(
            invoiceRepository.findById(invoiceId).orElseThrow().getIssuedAt()
        ).isNotNull();
    }

    @Test
    void regularUserShouldNotIssueInvoice() throws Exception {
        String adminToken = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String userToken = login("user.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(adminToken, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(userToken))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void crossTenantIssueShouldReturnNotFound() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(tokenTwo, companyTwoClient.getId(), companyTwoItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void issuedInvoiceShouldNotBeUpdated() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                put("/invoices/{invoiceId}", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedInvoicePayload(companyOneClient.getId(), companyOneItem.getId()))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Issued invoice cannot be modified."));
    }

    @Test
    void repeatedIssueShouldReturnConflict() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode createdInvoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", createdInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Issued invoice cannot be modified."));
    }

    @Test
    void invalidPayloadShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "clientId", companyOneClient.getId().toString(),
                        "issueDate", "2026-03-26",
                        "dueDate", "2026-03-19",
                        "currency", "rsd",
                        "lines", List.of()
                    )))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortFieldShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(token))
                    .param("sort", "unknown,asc")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid sort field."));
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

    private JsonNode createInvoice(String token, UUID clientId, UUID itemId) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(clientId, itemId, "2026-03-19", "2026-03-26"))
            )
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private String invoicePayload(UUID clientId, UUID itemId, String issueDate, String dueDate) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "clientId", clientId.toString(),
            "issueDate", issueDate,
            "dueDate", dueDate,
            "currency", "RSD",
            "note", "Draft invoice",
            "lines", List.of(Map.of(
                "itemId", itemId.toString(),
                "itemName", "Development Service",
                "itemCode", "DEV-001",
                "unit", "hour",
                "quantity", "4.000",
                "unitPrice", "100.00",
                "taxRate", "20.00"
            ))
        ));
    }

    private String updatedInvoicePayload(UUID clientId, UUID itemId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "clientId", clientId.toString(),
            "issueDate", "2026-03-20",
            "dueDate", "2026-03-27",
            "currency", "RSD",
            "note", "Updated draft",
            "lines", List.of(
                Map.of(
                    "itemId", itemId.toString(),
                    "itemName", "Development Service",
                    "itemCode", "DEV-001",
                    "unit", "hour",
                    "quantity", "2.000",
                    "unitPrice", "100.00",
                    "taxRate", "20.00"
                ),
                Map.of(
                    "itemName", "Additional Review",
                    "itemCode", "REV-001",
                    "unit", "hour",
                    "quantity", "1.000",
                    "unitPrice", "100.00",
                    "taxRate", "20.00"
                )
            )
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

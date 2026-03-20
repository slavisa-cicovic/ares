package com.sc.ares;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PaymentModuleIntegrationTests {

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

        companyOneClient = clientRepository.save(Client.create(
            companyOne, "Acme Client", "111", "222", "acme@local.ares", "123", "Street 1", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE
        ));
        companyTwoClient = clientRepository.save(Client.create(
            companyTwo, "Beta Client", "333", "444", "beta@local.ares", "456", "Neka 2", "Novi Sad", "21000", "Serbia", ClientStatus.ACTIVE
        ));
        companyOneItem = itemRepository.save(Item.create(
            companyOne, "Development Service", "DEV-001", "Monthly development", "hour",
            new BigDecimal("100.00"), new BigDecimal("20.00"), ItemStatus.ACTIVE
        ));
        companyTwoItem = itemRepository.save(Item.create(
            companyTwo, "Support Service", "SUP-001", "Support", "hour",
            new BigDecimal("80.00"), new BigDecimal("10.00"), ItemStatus.ACTIVE
        ));
    }

    @Test
    void companyAdminShouldCreatePaymentForOwnIssuedInvoice() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "120.00", "First payment"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(120.00))
            .andExpect(jsonPath("$.paymentDate").value("2026-03-22"));

        mockMvc.perform(
                get("/invoices/{invoiceId}", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"))
            .andExpect(jsonPath("$.paidAmount").value(120.00))
            .andExpect(jsonPath("$.outstandingAmount").value(360.00));
    }

    @Test
    void invoiceDetailShouldReturnUnpaidForIssuedInvoiceWithoutPayments() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                get("/invoices/{invoiceId}", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
            .andExpect(jsonPath("$.paidAmount").value(0.00))
            .andExpect(jsonPath("$.outstandingAmount").value(480.00));
    }

    @Test
    void invoiceDetailShouldReturnPaidWhenFullySettled() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "480.00", "Full payment"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices/{invoiceId}", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.paidAmount").value(480.00))
            .andExpect(jsonPath("$.outstandingAmount").value(0.00));
    }

    @Test
    void invoiceDetailShouldReturnUnpaidForDraftInvoice() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                get("/invoices/{invoiceId}", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
    }

    @Test
    void invoiceListShouldFilterByPaymentStatus() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode unpaidInvoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());
        JsonNode partialInvoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());
        JsonNode paidInvoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", partialInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "100.00", "Partial"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", paidInvoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "480.00", "Paid"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(token))
                    .param("paymentStatus", "UNPAID")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(unpaidInvoice.get("id").asText()))
            .andExpect(jsonPath("$.items[0].paymentStatus").value("UNPAID"));

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(token))
                    .param("paymentStatus", "PARTIALLY_PAID")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(partialInvoice.get("id").asText()))
            .andExpect(jsonPath("$.items[0].paymentStatus").value("PARTIALLY_PAID"));

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(token))
                    .param("paymentStatus", "PAID")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(paidInvoice.get("id").asText()))
            .andExpect(jsonPath("$.items[0].paymentStatus").value("PAID"));
    }

    @Test
    void invalidPaymentStatusFilterShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/invoices")
                    .header("Authorization", bearer(token))
                    .param("paymentStatus", "UNKNOWN")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid invoice payment status."));
    }

    @Test
    void regularUserShouldNotCreatePayment() throws Exception {
        String adminToken = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String userToken = login("user.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(adminToken, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "120.00", "First payment"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void regularUserShouldListOwnTenantPayments() throws Exception {
        String adminToken = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String userToken = login("user.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(adminToken, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-21", "100.00", "Earlier payment"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "120.00", "Later payment"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(userToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].paymentDate").value("2026-03-22"))
            .andExpect(jsonPath("$[1].paymentDate").value("2026-03-21"));
    }

    @Test
    void crossTenantPaymentAccessShouldReturnNotFound() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(tokenTwo, companyTwoClient.getId(), companyTwoItem.getId());

        mockMvc.perform(
                get("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
            )
            .andExpect(status().isNotFound());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "100.00", "Cross tenant"))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void paymentShouldBeRejectedForDraftInvoice() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "100.00", "Draft payment"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Payments can only be recorded for issued invoices."));
    }

    @Test
    void overpaymentShouldBeRejected() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode invoice = createAndIssueInvoice(token, companyOneClient.getId(), companyOneItem.getId());

        mockMvc.perform(
                post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload("2026-03-22", "500.00", "Too much"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Payment amount exceeds outstanding invoice amount."));
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

    private JsonNode createAndIssueInvoice(String token, UUID clientId, UUID itemId) throws Exception {
        JsonNode invoice = createInvoice(token, clientId, itemId);

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk());

        return invoice;
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

    private String paymentPayload(String paymentDate, String amount, String note) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "paymentDate", paymentDate,
            "amount", amount,
            "note", note
        ));
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

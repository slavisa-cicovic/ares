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
import com.sc.ares.features.refund.repository.CreditNoteRefundRepository;
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
class RefundModuleIntegrationTests {

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
    private CreditNoteRefundRepository creditNoteRefundRepository;

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
    void companyAdminShouldCreateRefundForFullyPaidCreditNote() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode creditNote = createIssuedCreditNote(token, companyOneClient.getId(), companyOneItem.getId(), "480.00");

        mockMvc.perform(
                post("/invoices/{invoiceId}/refunds", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refundPayload("2026-03-22", "480.00", "Refunded"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(480.00));

        mockMvc.perform(
                get("/invoices/{invoiceId}", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refundStatus").value("REFUNDED"))
            .andExpect(jsonPath("$.refundableAmount").value(480.00))
            .andExpect(jsonPath("$.refundedAmount").value(480.00))
            .andExpect(jsonPath("$.remainingRefundAmount").value(0.00));
    }

    @Test
    void refundShouldBeOptionalForUnpaidCreditNote() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode creditNote = createIssuedCreditNote(token, companyOneClient.getId(), companyOneItem.getId(), null);

        mockMvc.perform(
                get("/invoices/{invoiceId}", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refundStatus").value("NOT_REQUIRED"))
            .andExpect(jsonPath("$.refundableAmount").value(0.00))
            .andExpect(jsonPath("$.remainingRefundAmount").value(0.00));
    }

    @Test
    void partiallyPaidCreditNoteShouldAllowPartialRefund() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode creditNote = createIssuedCreditNote(token, companyOneClient.getId(), companyOneItem.getId(), "100.00");

        mockMvc.perform(
                post("/invoices/{invoiceId}/refunds", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refundPayload("2026-03-22", "40.00", "Partial refund"))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/invoices/{invoiceId}", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refundStatus").value("PARTIALLY_REFUNDED"))
            .andExpect(jsonPath("$.refundableAmount").value(100.00))
            .andExpect(jsonPath("$.refundedAmount").value(40.00))
            .andExpect(jsonPath("$.remainingRefundAmount").value(60.00));
    }

    @Test
    void refundShouldRejectAmountAboveRemainingRefundableAmount() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        JsonNode creditNote = createIssuedCreditNote(token, companyOneClient.getId(), companyOneItem.getId(), "100.00");

        mockMvc.perform(
                post("/invoices/{invoiceId}/refunds", creditNote.get("id").asText())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refundPayload("2026-03-22", "120.00", "Too much"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Refund amount exceeds remaining refundable amount."));
    }

    @Test
    void crossTenantRefundShouldReturnNotFound() throws Exception {
        String tokenOne = login("admin.one@local.ares", "ares-test").get("accessToken").asText();
        String tokenTwo = login("admin.two@local.ares", "ares-test").get("accessToken").asText();
        JsonNode creditNote = createIssuedCreditNote(tokenTwo, companyTwoClient.getId(), companyTwoItem.getId(), "480.00");

        mockMvc.perform(
                post("/invoices/{invoiceId}/refunds", creditNote.get("id").asText())
                    .header("Authorization", bearer(tokenOne))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refundPayload("2026-03-22", "100.00", "Cross tenant"))
            )
            .andExpect(status().isNotFound());
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

    private JsonNode createIssuedCreditNote(String token, UUID clientId, UUID itemId, String paymentAmount) throws Exception {
        JsonNode invoice = createInvoice(token, clientId, itemId);

        mockMvc.perform(
                post("/invoices/{invoiceId}/issue", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk());

        if (paymentAmount != null) {
            mockMvc.perform(
                    post("/invoices/{invoiceId}/payments", invoice.get("id").asText())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentPayload("2026-03-21", paymentAmount, "Payment"))
                )
                .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(
                post("/invoices/{invoiceId}/credit-note", invoice.get("id").asText())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createInvoice(String token, UUID clientId, UUID itemId) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/invoices")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invoicePayload(clientId, itemId))
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

    private String invoicePayload(UUID clientId, UUID itemId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "clientId", clientId.toString(),
            "issueDate", "2026-03-19",
            "dueDate", "2026-03-26",
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

    private String paymentPayload(String paymentDate, String amount, String note) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "paymentDate", paymentDate,
            "amount", amount,
            "note", note
        ));
    }

    private String refundPayload(String refundDate, String amount, String note) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "refundDate", refundDate,
            "amount", amount,
            "note", note
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

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
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.entity.ClientStatus;
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
class ClientModuleIntegrationTests {

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
    private Client companyOneClient;
    private Client companyTwoClient;

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

        createUser(companyOne, "admin.one@local.ares", companyAdminRole);
        createUser(companyOne, "user.one@local.ares", userRole);
        createUser(companyTwo, "admin.two@local.ares", companyAdminRole);

        companyOneClient = clientRepository.save(Client.create(
            companyOne, "Acme", "111", "222", "acme@local.ares", "123", "Street 1", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE
        ));
        companyTwoClient = clientRepository.save(Client.create(
            companyTwo, "Beta Client", "333", "444", "beta@local.ares", "456", "Street 2", "Novi Sad", "21000", "Serbia", ClientStatus.ACTIVE
        ));
    }

    @Test
    void companyAdminShouldCreateClientInOwnCompany() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/clients")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(clientPayload("New Client", "ACTIVE"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("New Client"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void regularUserShouldReadOwnCompanyClients() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/clients")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Acme"));
    }

    @Test
    void regularUserShouldNotCreateClient() throws Exception {
        String token = login("user.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                post("/clients")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(clientPayload("Nope", "ACTIVE"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void clientListShouldReturnOnlyOwnCompanyClients() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/clients")
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Acme"));
    }

    @Test
    void clientListSearchShouldMatchBusinessFieldsBeyondNameAndEmail() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/clients")
                    .header("Authorization", bearer(token))
                    .param("search", "222")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Acme"));

        mockMvc.perform(
                get("/clients")
                    .header("Authorization", bearer(token))
                    .param("search", "Street 1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Acme"));

        mockMvc.perform(
                get("/clients")
                    .header("Authorization", bearer(token))
                    .param("search", "Belgrade")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Acme"));
    }

    @Test
    void crossTenantDetailShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/clients/{clientId}", companyTwoClient.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantUpdateShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/clients/{clientId}", companyTwoClient.getId())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(clientPayload("Updated", "ACTIVE"))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantDeleteShouldReturnNotFound() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/clients/{clientId}", companyTwoClient.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void companyAdminShouldUpdateOwnClient() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                put("/clients/{clientId}", companyOneClient.getId())
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(clientPayload("Acme Updated", "INACTIVE"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Acme Updated"))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void softDeletedClientShouldDisappearFromNormalReads() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                delete("/clients/{clientId}", companyOneClient.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/clients/{clientId}", companyOneClient.getId())
                    .header("Authorization", bearer(token))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidSortFieldShouldReturnBadRequest() throws Exception {
        String token = login("admin.one@local.ares", "ares-test").get("accessToken").asText();

        mockMvc.perform(
                get("/clients")
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
                post("/clients")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "name", "",
                        "status", "ACTIVE"
                    )))
            )
            .andExpect(status().isBadRequest());
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

    private String clientPayload(String name, String status) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "name", name,
            "pib", "123456",
            "mb", "654321",
            "email", "client@local.ares",
            "phone", "12345",
            "address", "Main Street",
            "city", "Belgrade",
            "postalCode", "11000",
            "country", "Serbia",
            "status", status
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

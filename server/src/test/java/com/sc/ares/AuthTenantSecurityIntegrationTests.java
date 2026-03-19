package com.sc.ares;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.tenant.TenantContext;
import com.sc.ares.common.tenant.TenantContextService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthTenantSecurityIntegrationTests.TestSecurityEndpointsConfiguration.class)
class AuthTenantSecurityIntegrationTests {

    private static final UUID COMPANY_ONE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COMPANY_TWO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicHealthEndpointShouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/public/health"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(get("/test/authenticated"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void companyAdminEndpointShouldReturnForbiddenForRegularUser() throws Exception {
        mockMvc.perform(
                get("/test/company-admin")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("user.one@local.ares", COMPANY_ONE_ID, false, "ROLE_USER")
                    ))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void tenantEndpointShouldAllowSameTenantAccessForCompanyAdmin() throws Exception {
        mockMvc.perform(
                get("/test/tenant/{companyId}", COMPANY_ONE_ID)
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("admin.one@local.ares", COMPANY_ONE_ID, false, "ROLE_COMPANY_ADMIN")
                    ))
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }

    @Test
    void tenantEndpointShouldRejectCrossTenantAccessForCompanyAdmin() throws Exception {
        mockMvc.perform(
                get("/test/tenant/{companyId}", COMPANY_TWO_ID)
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("admin.one@local.ares", COMPANY_ONE_ID, false, "ROLE_COMPANY_ADMIN")
                    ))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void platformOnlyEndpointShouldAllowSuperAdmin() throws Exception {
        mockMvc.perform(
                get("/test/platform-only")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("superadmin@local.ares", COMPANY_ONE_ID, true, "ROLE_SUPER_ADMIN")
                    ))
            )
            .andExpect(status().isOk());
    }

    @Test
    void platformOnlyEndpointShouldRejectCompanyAdmin() throws Exception {
        mockMvc.perform(
                get("/test/platform-only")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("admin.one@local.ares", COMPANY_ONE_ID, false, "ROLE_COMPANY_ADMIN")
                    ))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void superAdminShouldBeAllowedAcrossTenantsWhenExplicitlyPermitted() throws Exception {
        mockMvc.perform(
                get("/test/tenant/{companyId}", COMPANY_TWO_ID)
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        authentication("superadmin@local.ares", COMPANY_ONE_ID, true, "ROLE_SUPER_ADMIN")
                    ))
            )
            .andExpect(status().isOk());
    }

    private Authentication authentication(
        String email,
        UUID companyId,
        boolean platformAdmin,
        String... authorities
    ) {
        AresAuthenticatedPrincipal principal = new AresAuthenticatedPrincipal(
            UUID.randomUUID(),
            companyId,
            email,
            platformAdmin,
            List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList()
        );

        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            "n/a",
            principal.authorities()
        );
    }

    @TestConfiguration
    static class TestSecurityEndpointsConfiguration {

        @Bean
        TestSecurityController testSecurityController(TenantContextService tenantContextService) {
            return new TestSecurityController(tenantContextService);
        }
    }

    @RestController
    @RequestMapping("/test")
    static class TestSecurityController {

        private final TenantContextService tenantContextService;

        TestSecurityController(TenantContextService tenantContextService) {
            this.tenantContextService = tenantContextService;
        }

        @GetMapping("/authenticated")
        Map<String, String> authenticated() {
            return Map.of("status", "ok");
        }

        @GetMapping("/company-admin")
        Map<String, String> companyAdmin() {
            TenantContext context = tenantContextService.getRequiredTenantContext();

            if (!hasAuthority("ROLE_COMPANY_ADMIN") || context.platformAdmin()) {
                throw new AccessDeniedException("Company admin access is required.");
            }

            return Map.of("status", "ok");
        }

        @GetMapping("/platform-only")
        Map<String, String> platformOnly() {
            TenantContext context = tenantContextService.getRequiredTenantContext();

            if (!context.platformAdmin() || !hasAuthority("ROLE_SUPER_ADMIN")) {
                throw new AccessDeniedException("Super admin access is required.");
            }

            return Map.of("status", "ok");
        }

        @GetMapping("/tenant/{companyId}")
        Map<String, String> tenantScoped(@PathVariable UUID companyId) {
            TenantContext context = tenantContextService.getRequiredTenantContext();

            if (!context.platformAdmin() && !context.companyId().equals(companyId)) {
                throw new AccessDeniedException("Cross-tenant access is forbidden.");
            }

            return Map.of("status", "ok");
        }

        private boolean hasAuthority(String authority) {
            Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

            return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
        }
    }
}

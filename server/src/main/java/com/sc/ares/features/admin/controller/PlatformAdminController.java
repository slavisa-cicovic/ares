package com.sc.ares.features.admin.controller;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.company.dto.CompanySummaryResponse;
import com.sc.ares.features.company.dto.CreateCompanyRequest;
import com.sc.ares.features.company.service.CompanyManagementService;
import com.sc.ares.features.user.dto.CreatePlatformUserRequest;
import com.sc.ares.features.user.dto.PlatformCreatedUserResponse;
import com.sc.ares.features.user.dto.PlatformUserResponse;
import com.sc.ares.features.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class PlatformAdminController {

    private final CompanyManagementService companyManagementService;
    private final UserManagementService userManagementService;

    public PlatformAdminController(
        CompanyManagementService companyManagementService,
        UserManagementService userManagementService
    ) {
        this.companyManagementService = companyManagementService;
        this.userManagementService = userManagementService;
    }

    @GetMapping("/companies")
    public PagedResponse<CompanySummaryResponse> listCompanies(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @RequestParam(required = false) String search
    ) {
        return companyManagementService.listPlatformCompanies(page, size, sort, search);
    }

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanySummaryResponse createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return companyManagementService.createPlatformCompany(request);
    }

    @GetMapping("/users")
    public PagedResponse<PlatformUserResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        return userManagementService.listPlatformUsers(page, size, sort, status, search);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformCreatedUserResponse createUser(@Valid @RequestBody CreatePlatformUserRequest request) {
        return userManagementService.createPlatformUser(request);
    }
}

package com.sc.ares.features.company.controller;

import com.sc.ares.features.company.dto.CompanyResponse;
import com.sc.ares.features.company.dto.UpdateCompanyRequest;
import com.sc.ares.features.company.service.CompanyManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyManagementService companyManagementService;

    public CompanyController(CompanyManagementService companyManagementService) {
        this.companyManagementService = companyManagementService;
    }

    @GetMapping
    public CompanyResponse getCurrentCompany() {
        return companyManagementService.getCurrentCompany();
    }

    @PutMapping
    public CompanyResponse updateCurrentCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        return companyManagementService.updateCurrentCompany(request);
    }
}

package com.sc.ares.features.company.controller;

import com.sc.ares.features.company.dto.CompanyLogoUploadResponse;
import com.sc.ares.features.company.dto.CompanyResponse;
import com.sc.ares.features.company.dto.UpdateCompanyRequest;
import com.sc.ares.features.company.service.CompanyLogoStorageService;
import com.sc.ares.features.company.service.CompanyManagementService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyManagementService companyManagementService;
    private final CompanyLogoStorageService companyLogoStorageService;

    public CompanyController(
        CompanyManagementService companyManagementService,
        CompanyLogoStorageService companyLogoStorageService
    ) {
        this.companyManagementService = companyManagementService;
        this.companyLogoStorageService = companyLogoStorageService;
    }

    @GetMapping
    public CompanyResponse getCurrentCompany() {
        return companyManagementService.getCurrentCompany();
    }

    @PutMapping
    public CompanyResponse updateCurrentCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        return companyManagementService.updateCurrentCompany(request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanyLogoUploadResponse uploadCompanyLogo(@RequestParam("file") MultipartFile file) {
        String fileName = companyManagementService.storeCurrentCompanyLogo(file);
        String logoUrl = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath("/api/company/logo/" + fileName)
            .toUriString();

        return new CompanyLogoUploadResponse(logoUrl);
    }

    @GetMapping("/logo/{fileName}")
    public ResponseEntity<Resource> getCompanyLogo(@PathVariable String fileName) {
        Resource logoResource = companyLogoStorageService.getLogoResource(fileName);

        return ResponseEntity.ok()
            .contentType(companyLogoStorageService.getMediaType(fileName))
            .cacheControl(CacheControl.noCache())
            .body(logoResource);
    }
}

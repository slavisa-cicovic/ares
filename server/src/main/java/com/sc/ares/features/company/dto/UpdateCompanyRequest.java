package com.sc.ares.features.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
    @NotBlank
    @Size(max = 255)
    String name
) {
}

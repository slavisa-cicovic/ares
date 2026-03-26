package com.sc.ares.features.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
    @NotBlank
    @Size(max = 255)
    String name,
    @NotBlank
    @Size(max = 64)
    String pib,
    @NotBlank
    @Size(max = 64)
    String mb
) {
}

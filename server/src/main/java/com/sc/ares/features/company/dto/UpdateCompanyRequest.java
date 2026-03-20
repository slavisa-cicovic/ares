package com.sc.ares.features.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
    @NotBlank
    @Size(max = 255)
    String name,
    @Size(max = 64)
    String pib,
    @Size(max = 64)
    String mb,
    @Size(max = 255)
    String address,
    @Size(max = 320)
    String email,
    @Size(max = 64)
    String phone,
    @Size(max = 128)
    String bankAccount
) {
}

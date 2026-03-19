package com.sc.ares.features.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertClientRequest(
    @NotBlank
    @Size(max = 255)
    String name,
    @Size(max = 32)
    String pib,
    @Size(max = 32)
    String mb,
    @Email
    @Size(max = 320)
    String email,
    @Size(max = 50)
    String phone,
    @Size(max = 255)
    String address,
    @Size(max = 120)
    String city,
    @Size(max = 20)
    String postalCode,
    @Size(max = 120)
    String country,
    @NotBlank
    String status
) {
}

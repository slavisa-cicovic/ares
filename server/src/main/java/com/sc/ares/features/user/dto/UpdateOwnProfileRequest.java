package com.sc.ares.features.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
    @NotBlank
    @Email
    String email,
    @NotBlank
    @Size(max = 100)
    String firstName,
    @NotBlank
    @Size(max = 100)
    String lastName,
    @Size(max = 64)
    String phone,
    @Size(max = 512)
    String userLogoUrl
) {
}

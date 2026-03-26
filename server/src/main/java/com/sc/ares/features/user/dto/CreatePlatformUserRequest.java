package com.sc.ares.features.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreatePlatformUserRequest(
    @NotNull
    UUID companyId,
    @NotBlank
    @Email
    String email,
    @NotBlank
    @Size(max = 100)
    String firstName,
    @NotBlank
    @Size(max = 100)
    String lastName,
    @NotBlank
    String status,
    @NotEmpty
    List<@NotBlank String> roles
) {
}

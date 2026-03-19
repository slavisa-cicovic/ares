package com.sc.ares.features.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUserRequest(
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

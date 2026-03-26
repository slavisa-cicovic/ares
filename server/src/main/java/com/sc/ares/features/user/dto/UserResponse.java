package com.sc.ares.features.user.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    UUID companyId,
    String companyName,
    String email,
    String firstName,
    String lastName,
    String status,
    List<String> roles
) {
}

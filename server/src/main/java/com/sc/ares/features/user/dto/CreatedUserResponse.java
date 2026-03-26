package com.sc.ares.features.user.dto;

import java.util.List;
import java.util.UUID;

public record CreatedUserResponse(
    UUID id,
    UUID companyId,
    String companyName,
    String email,
    String firstName,
    String lastName,
    String status,
    List<String> roles,
    String temporaryPassword
) {
}

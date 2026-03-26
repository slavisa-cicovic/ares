package com.sc.ares.features.user.dto;

import java.util.List;
import java.util.UUID;

public record PlatformUserResponse(
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

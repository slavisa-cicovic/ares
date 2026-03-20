package com.sc.ares.features.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUserResponse(
    UUID userId,
    String email,
    String firstName,
    String lastName,
    UUID companyId,
    String companyName,
    String companyBrandColor,
    String companyLogoUrl,
    String companyThemePreset,
    List<String> roles,
    boolean platformAdmin
) {
}

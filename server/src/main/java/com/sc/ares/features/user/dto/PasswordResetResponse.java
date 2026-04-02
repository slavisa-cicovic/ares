package com.sc.ares.features.user.dto;

import java.util.UUID;

public record PasswordResetResponse(
    UUID userId,
    UUID companyId,
    String companyName,
    String email,
    String temporaryPassword
) {
}

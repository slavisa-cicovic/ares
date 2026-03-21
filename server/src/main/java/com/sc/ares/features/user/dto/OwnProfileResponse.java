package com.sc.ares.features.user.dto;

import java.util.List;
import java.util.UUID;

public record OwnProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    String userLogoUrl,
    String status,
    List<String> roles
) {
}

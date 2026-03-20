package com.sc.ares.features.company.dto;

import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String pib,
    String mb,
    String address,
    String email,
    String phone,
    String bankAccount,
    String brandColor,
    String logoUrl,
    String themePreset,
    String status
) {
}

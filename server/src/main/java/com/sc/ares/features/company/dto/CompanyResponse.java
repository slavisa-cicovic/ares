package com.sc.ares.features.company.dto;

import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String status
) {
}

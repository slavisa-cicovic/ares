package com.sc.ares.features.company.dto;

import java.util.UUID;

public record CompanySummaryResponse(
    UUID id,
    String name,
    String pib,
    String mb,
    String status
) {
}

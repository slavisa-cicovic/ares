package com.sc.ares.features.client.dto;

import java.util.UUID;

public record ClientResponse(
    UUID id,
    String name,
    String pib,
    String mb,
    String email,
    String phone,
    String address,
    String city,
    String postalCode,
    String country,
    String status
) {
}

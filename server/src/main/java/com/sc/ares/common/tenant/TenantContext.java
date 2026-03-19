package com.sc.ares.common.tenant;

import java.util.UUID;

public record TenantContext(
    UUID userId,
    UUID companyId,
    boolean platformAdmin
) {
}

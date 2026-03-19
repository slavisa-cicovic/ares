package com.sc.ares.common.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public record AresAuthenticatedPrincipal(
    UUID userId,
    UUID companyId,
    String email,
    boolean platformAdmin,
    Collection<? extends GrantedAuthority> authorities
) implements Serializable {
}

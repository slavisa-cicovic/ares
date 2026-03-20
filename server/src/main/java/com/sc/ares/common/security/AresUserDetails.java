package com.sc.ares.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AresUserDetails implements UserDetails {

    private final AresAuthenticatedPrincipal principal;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final String companyName;
    private final String companyBrandColor;
    private final String companyLogoUrl;
    private final String companyThemePreset;
    private final List<String> roleCodes;
    private final boolean enabled;

    public AresUserDetails(
        AresAuthenticatedPrincipal principal,
        String passwordHash,
        String firstName,
        String lastName,
        String companyName,
        String companyBrandColor,
        String companyLogoUrl,
        String companyThemePreset,
        List<String> roleCodes,
        boolean enabled
    ) {
        this.principal = principal;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyName = companyName;
        this.companyBrandColor = companyBrandColor;
        this.companyLogoUrl = companyLogoUrl;
        this.companyThemePreset = companyThemePreset;
        this.roleCodes = List.copyOf(roleCodes);
        this.enabled = enabled;
    }

    public AresAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyBrandColor() {
        return companyBrandColor;
    }

    public String getCompanyLogoUrl() {
        return companyLogoUrl;
    }

    public String getCompanyThemePreset() {
        return companyThemePreset;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.authorities();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return principal.email();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

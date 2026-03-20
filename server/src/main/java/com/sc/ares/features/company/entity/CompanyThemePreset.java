package com.sc.ares.features.company.entity;

public enum CompanyThemePreset {
    AMBER,
    OCEAN,
    GRAPHITE;

    public String toApiValue() {
        return name().toLowerCase();
    }

    public static CompanyThemePreset fromApiValue(String value) {
        if (value == null) {
            return AMBER;
        }

        return CompanyThemePreset.valueOf(value.trim().toUpperCase());
    }
}

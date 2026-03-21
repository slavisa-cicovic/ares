package com.sc.ares.features.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 64)
    private String pib;

    @Column(length = 64)
    private String mb;

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(length = 128)
    private String city;

    @Column(length = 128)
    private String country;

    @Column(length = 320)
    private String email;

    @Column(length = 64)
    private String phone;

    @Column(name = "bank_account", length = 128)
    private String bankAccount;

    @Column(name = "brand_color", length = 7)
    private String brandColor;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preset", nullable = false, length = 32)
    private CompanyThemePreset themePreset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompanyStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public static Company create(String name, CompanyStatus status) {
        Instant now = Instant.now();

        Company company = new Company();
        company.name = name;
        company.themePreset = CompanyThemePreset.AMBER;
        company.status = status;
        company.createdAt = now;
        company.updatedAt = now;

        return company;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPib() {
        return pib;
    }

    public String getMb() {
        return mb;
    }

    public String getAddress() {
        return address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public CompanyThemePreset getThemePreset() {
        return themePreset;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void updateStatus(CompanyStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(
        String name,
        String pib,
        String mb,
        String address,
        String postalCode,
        String city,
        String country,
        String email,
        String phone,
        String bankAccount,
        String brandColor,
        String logoUrl,
        CompanyThemePreset themePreset
    ) {
        this.name = name;
        this.pib = pib;
        this.mb = mb;
        this.address = address;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.email = email;
        this.phone = phone;
        this.bankAccount = bankAccount;
        this.brandColor = brandColor;
        this.logoUrl = logoUrl;
        this.themePreset = themePreset;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}

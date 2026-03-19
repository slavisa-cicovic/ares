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

    public void softDelete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}

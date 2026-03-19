package com.sc.ares.features.auth.entity;

import com.sc.ares.features.company.entity.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoleScope scope;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {
    }

    public static Role create(
        Company company,
        String code,
        String name,
        RoleScope scope,
        boolean systemRole
    ) {
        Instant now = Instant.now();

        Role role = new Role();
        role.company = company;
        role.code = code;
        role.name = name;
        role.scope = scope;
        role.systemRole = systemRole;
        role.createdAt = now;
        role.updatedAt = now;

        return role;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public RoleScope getScope() {
        return scope;
    }

    public boolean isSystemRole() {
        return systemRole;
    }
}

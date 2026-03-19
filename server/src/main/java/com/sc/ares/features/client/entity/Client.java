package com.sc.ares.features.client.entity;

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
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 32)
    private String pib;

    @Column(length = 32)
    private String mb;

    @Column(length = 320)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 120)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClientStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Client() {
    }

    public static Client create(
        Company company,
        String name,
        String pib,
        String mb,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        ClientStatus status
    ) {
        Instant now = Instant.now();

        Client client = new Client();
        client.company = company;
        client.name = name;
        client.pib = pib;
        client.mb = mb;
        client.email = email;
        client.phone = phone;
        client.address = address;
        client.city = city;
        client.postalCode = postalCode;
        client.country = country;
        client.status = status;
        client.createdAt = now;
        client.updatedAt = now;

        return client;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void update(
        String name,
        String pib,
        String mb,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        ClientStatus status
    ) {
        this.name = name;
        this.pib = pib;
        this.mb = mb;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}

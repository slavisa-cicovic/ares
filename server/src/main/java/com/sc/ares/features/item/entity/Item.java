package com.sc.ares.features.item.entity;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 64)
    private String code;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ItemStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Item() {
    }

    public static Item create(
        Company company,
        String name,
        String code,
        String description,
        String unit,
        BigDecimal price,
        BigDecimal taxRate,
        ItemStatus status
    ) {
        Instant now = Instant.now();

        Item item = new Item();
        item.company = company;
        item.name = name;
        item.code = code;
        item.description = description;
        item.unit = unit;
        item.price = price;
        item.taxRate = taxRate;
        item.status = status;
        item.createdAt = now;
        item.updatedAt = now;

        return item;
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

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void update(
        String name,
        String code,
        String description,
        String unit,
        BigDecimal price,
        BigDecimal taxRate,
        ItemStatus status
    ) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.unit = unit;
        this.price = price;
        this.taxRate = taxRate;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}

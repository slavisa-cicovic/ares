package com.sc.ares.features.item.entity;

import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.user.entity.AppUser;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

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

    @Column(name = "default_value", nullable = false, precision = 15, scale = 3)
    private BigDecimal defaultValue;

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
        AppUser createdByUser,
        String name,
        String code,
        String description,
        String unit,
        BigDecimal price,
        BigDecimal taxRate,
        BigDecimal defaultValue,
        ItemStatus status
    ) {
        Instant now = Instant.now();

        Item item = new Item();
        item.company = company;
        item.createdByUser = createdByUser;
        item.name = name;
        item.code = code;
        item.description = description;
        item.unit = unit;
        item.price = price;
        item.taxRate = taxRate;
        item.defaultValue = defaultValue;
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

    public AppUser getCreatedByUser() {
        return createdByUser;
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

    public BigDecimal getDefaultValue() {
        return defaultValue;
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
        BigDecimal defaultValue,
        ItemStatus status
    ) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.unit = unit;
        this.price = price;
        this.taxRate = taxRate;
        this.defaultValue = defaultValue;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}

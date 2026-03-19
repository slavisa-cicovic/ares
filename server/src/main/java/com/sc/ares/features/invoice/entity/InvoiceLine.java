package com.sc.ares.features.invoice.entity;

import com.sc.ares.features.item.entity.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "invoice_lines")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(name = "line_tax_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTaxTotal;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvoiceLine() {
    }

    public static InvoiceLine create(
        Item item,
        Integer lineNumber,
        String itemName,
        String itemCode,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal lineTaxTotal,
        BigDecimal lineTotal
    ) {
        InvoiceLine line = new InvoiceLine();
        line.item = item;
        line.lineNumber = lineNumber;
        line.itemName = itemName;
        line.itemCode = itemCode;
        line.unit = unit;
        line.quantity = quantity;
        line.unitPrice = unitPrice;
        line.taxRate = taxRate;
        line.lineSubtotal = lineSubtotal;
        line.lineTaxTotal = lineTaxTotal;
        line.lineTotal = lineTotal;
        line.createdAt = Instant.now();

        return line;
    }

    public UUID getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal getLineTaxTotal() {
        return lineTaxTotal;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    void attachTo(Invoice invoice) {
        this.invoice = invoice;
    }
}

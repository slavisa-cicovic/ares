package com.sc.ares.features.invoice.entity;

import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.company.entity.Company;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "invoice_number", nullable = false, length = 32)
    private String invoiceNumber;

    @Column(name = "sequence_year", nullable = false)
    private int sequenceYear;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxTotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
    }

    public static Invoice create(
        Company company,
        Client client,
        String invoiceNumber,
        int sequenceYear,
        int sequenceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        String currency,
        String note,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total
    ) {
        Instant now = Instant.now();

        Invoice invoice = new Invoice();
        invoice.company = company;
        invoice.client = client;
        invoice.invoiceNumber = invoiceNumber;
        invoice.sequenceYear = sequenceYear;
        invoice.sequenceNumber = sequenceNumber;
        invoice.issueDate = issueDate;
        invoice.dueDate = dueDate;
        invoice.currency = currency;
        invoice.status = InvoiceStatus.DRAFT;
        invoice.note = note;
        invoice.subtotal = subtotal;
        invoice.taxTotal = taxTotal;
        invoice.total = total;
        invoice.createdAt = now;
        invoice.updatedAt = now;

        return invoice;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public Client getClient() {
        return client;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getCurrency() {
        return currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void updateDraft(
        Client client,
        LocalDate issueDate,
        LocalDate dueDate,
        String currency,
        String note,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total
    ) {
        this.client = client;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.currency = currency;
        this.note = note;
        this.subtotal = subtotal;
        this.taxTotal = taxTotal;
        this.total = total;
        this.updatedAt = Instant.now();
    }

    public void replaceLines(List<InvoiceLine> newLines) {
        this.lines.clear();

        for (InvoiceLine line : newLines) {
            line.attachTo(this);
            this.lines.add(line);
        }

        this.updatedAt = Instant.now();
    }
}

package com.sc.ares.features.invoice.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.invoice.dto.InvoiceLineResponse;
import com.sc.ares.features.invoice.dto.InvoiceResponse;
import com.sc.ares.features.invoice.dto.InvoiceSummaryResponse;
import com.sc.ares.features.invoice.dto.UpsertInvoiceLineRequest;
import com.sc.ares.features.invoice.dto.UpsertInvoiceRequest;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoiceLine;
import com.sc.ares.features.invoice.entity.InvoicePaymentStatus;
import com.sc.ares.features.invoice.entity.InvoiceRefundStatus;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.item.entity.Item;
import com.sc.ares.features.item.repository.ItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "invoiceNumber", "invoiceNumber",
        "issueDate", "issueDate",
        "dueDate", "dueDate",
        "total", "total",
        "createdAt", "createdAt"
    );

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final InvoicePaymentStateService invoicePaymentStateService;
    private final InvoiceRefundStateService invoiceRefundStateService;
    private final InvoiceAccessService invoiceAccessService;

    public InvoiceService(
        InvoiceRepository invoiceRepository,
        CompanyRepository companyRepository,
        ClientRepository clientRepository,
        ItemRepository itemRepository,
        InvoicePaymentStateService invoicePaymentStateService,
        InvoiceRefundStateService invoiceRefundStateService,
        InvoiceAccessService invoiceAccessService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.itemRepository = itemRepository;
        this.invoicePaymentStateService = invoicePaymentStateService;
        this.invoiceRefundStateService = invoiceRefundStateService;
        this.invoiceAccessService = invoiceAccessService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceSummaryResponse> listInvoices(
        int page,
        int size,
        String sort,
        String status,
        String paymentStatus,
        String search
    ) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        List<Invoice> invoices = invoiceRepository.findAllByCompanyIdAndFilters(
            companyId,
            parseOptionalStatus(status),
            normalizeSearch(search),
            parseSort(sort)
        );
        Map<UUID, BigDecimal> paidAmounts = invoicePaymentStateService.getPaidAmounts(invoices);
        InvoicePaymentStatus requestedPaymentStatus = parseOptionalPaymentStatus(paymentStatus);
        List<InvoiceSummaryResponse> filteredItems = invoices.stream()
            .map(invoice -> toSummaryResponse(invoice, paidAmounts))
            .filter(summary -> matchesPaymentStatus(summary, requestedPaymentStatus))
            .toList();
        int fromIndex = Math.min(normalizedPage * normalizedSize, filteredItems.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filteredItems.size());
        int totalItems = filteredItems.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / normalizedSize);

        return new PagedResponse<>(
            filteredItems.subList(fromIndex, toIndex),
            normalizedPage,
            normalizedSize,
            totalItems,
            totalPages
        );
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        return toResponse(invoice, invoicePaymentStateService.getPaidAmount(invoice));
    }

    @Transactional
    public InvoiceResponse createInvoice(UpsertInvoiceRequest request) {
        Company company = getWritableCompany();
        validateDates(request.issueDate(), request.dueDate());
        Client client = getRequiredClient(request.clientId(), company.getId());
        InvoiceDraft draft = buildDraft(request.lines(), company.getId());

        int sequenceYear = request.issueDate().getYear();
        int nextSequenceNumber = invoiceRepository.findFirstByCompanyIdAndSequenceYearOrderBySequenceNumberDesc(
            company.getId(),
            sequenceYear
        ).map(invoice -> invoice.getSequenceNumber() + 1).orElse(1);

        Invoice invoice = Invoice.create(
            company,
            client,
            formatInvoiceNumber(sequenceYear, nextSequenceNumber),
            sequenceYear,
            nextSequenceNumber,
            request.issueDate(),
            request.dueDate(),
            normalizeCurrency(request.currency()),
            normalizeOptional(request.note()),
            draft.subtotal(),
            draft.taxTotal(),
            draft.total()
        );
        invoice.replaceLines(draft.lines());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return toResponse(savedInvoice, invoicePaymentStateService.getPaidAmount(savedInvoice));
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID invoiceId, UpsertInvoiceRequest request) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureDraft(invoice);

        validateDates(request.issueDate(), request.dueDate());
        Client client = getRequiredClient(request.clientId(), companyId);
        InvoiceDraft draft = buildDraft(request.lines(), companyId);

        invoice.updateDraft(
            client,
            request.issueDate(),
            request.dueDate(),
            normalizeCurrency(request.currency()),
            normalizeOptional(request.note()),
            draft.subtotal(),
            draft.taxTotal(),
            draft.total()
        );
        invoice.replaceLines(draft.lines());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return toResponse(savedInvoice, invoicePaymentStateService.getPaidAmount(savedInvoice));
    }

    @Transactional
    public InvoiceResponse issueInvoice(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureDraft(invoice);

        invoice.issue();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return toResponse(savedInvoice, invoicePaymentStateService.getPaidAmount(savedInvoice));
    }

    @Transactional
    public InvoiceResponse createCreditNote(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice originalInvoice = getRequiredInvoice(invoiceId, companyId);

        ensureCreditNoteAllowed(originalInvoice, companyId);

        LocalDate issueDate = LocalDate.now();
        int sequenceYear = issueDate.getYear();
        int nextSequenceNumber = invoiceRepository.findFirstByCompanyIdAndSequenceYearOrderBySequenceNumberDesc(
            companyId,
            sequenceYear
        ).map(invoice -> invoice.getSequenceNumber() + 1).orElse(1);

        Invoice creditNote = Invoice.createCreditNote(
            originalInvoice,
            formatInvoiceNumber(sequenceYear, nextSequenceNumber),
            sequenceYear,
            nextSequenceNumber,
            issueDate,
            issueDate,
            formatCreditNoteNote(originalInvoice),
            originalInvoice.getSubtotal().negate(),
            originalInvoice.getTaxTotal().negate(),
            originalInvoice.getTotal().negate()
        );
        creditNote.replaceLines(originalInvoice.getLines().stream().map(InvoiceLine::createCreditNoteLine).toList());

        Invoice savedCreditNote = invoiceRepository.save(creditNote);
        return toResponse(savedCreditNote, invoicePaymentStateService.getPaidAmount(savedCreditNote));
    }

    private Company getWritableCompany() {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    private Invoice getRequiredInvoice(UUID invoiceId, UUID companyId) {
        return invoiceRepository.findByIdAndCompanyIdAndDeletedAtIsNull(invoiceId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }

    private void ensureDraft(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessConflictException("Issued invoice cannot be modified.");
        }
    }

    private void ensureCreditNoteAllowed(Invoice invoice, UUID companyId) {
        if (invoice.getDocumentType() != InvoiceDocumentType.INVOICE || invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessConflictException("Only issued invoices can produce a credit note.");
        }

        if (invoiceRepository.findByOriginalInvoiceIdAndCompanyIdAndDeletedAtIsNull(invoice.getId(), companyId).isPresent()) {
            throw new BusinessConflictException("Credit note already exists for this invoice.");
        }
    }

    private Client getRequiredClient(UUID clientId, UUID companyId) {
        return clientRepository.findByIdAndCompanyIdAndDeletedAtIsNull(clientId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found."));
    }

    private InvoiceDraft buildDraft(List<UpsertInvoiceLineRequest> lineRequests, UUID companyId) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new ApiBadRequestException("At least one invoice line is required.");
        }

        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = money(BigDecimal.ZERO);
        BigDecimal taxTotal = money(BigDecimal.ZERO);
        BigDecimal total = money(BigDecimal.ZERO);

        for (int index = 0; index < lineRequests.size(); index++) {
            UpsertInvoiceLineRequest request = lineRequests.get(index);
            Item item = resolveOptionalItem(request.itemId(), companyId);

            String itemName = normalizeRequired(request.itemName(), "Required invoice line field is missing.");
            String itemCode = normalizeOptional(request.itemCode());
            String unit = normalizeRequired(request.unit(), "Required invoice line field is missing.");
            BigDecimal quantity = normalizeQuantity(request.quantity());
            BigDecimal unitPrice = normalizeMoney(request.unitPrice(), "Required invoice line field is missing.");
            BigDecimal taxRate = normalizeTaxRate(request.taxRate());

            if (itemCode == null && item != null) {
                itemCode = item.getCode();
            }

            BigDecimal lineSubtotal = money(quantity.multiply(unitPrice));
            BigDecimal lineTaxTotal = money(lineSubtotal.multiply(taxRate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
            BigDecimal lineTotal = money(lineSubtotal.add(lineTaxTotal));

            subtotal = money(subtotal.add(lineSubtotal));
            taxTotal = money(taxTotal.add(lineTaxTotal));
            total = money(total.add(lineTotal));

            lines.add(InvoiceLine.create(
                item,
                index + 1,
                itemName,
                itemCode,
                unit,
                quantity,
                unitPrice,
                taxRate,
                lineSubtotal,
                lineTaxTotal,
                lineTotal
            ));
        }

        return new InvoiceDraft(lines, subtotal, taxTotal, total);
    }

    private Item resolveOptionalItem(UUID itemId, UUID companyId) {
        if (itemId == null) {
            return null;
        }

        return itemRepository.findByIdAndCompanyIdAndDeletedAtIsNull(itemId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found."));
    }

    private InvoiceSummaryResponse toSummaryResponse(Invoice invoice, Map<UUID, BigDecimal> paidAmounts) {
        BigDecimal paidAmount = paidAmounts.getOrDefault(invoice.getId(), money(BigDecimal.ZERO));
        InvoicePaymentStatus paymentStatus = invoicePaymentStateService.derivePaymentStatus(invoice, paidAmount);
        BigDecimal refundedAmount = invoiceRefundStateService.getRefundedAmount(invoice);
        BigDecimal refundableAmount = invoiceRefundStateService.getRefundableAmount(invoice);
        BigDecimal remainingRefundAmount = invoiceRefundStateService.getRemainingRefundAmount(invoice, refundedAmount);
        InvoiceRefundStatus refundStatus = invoiceRefundStateService.deriveRefundStatus(invoice, refundableAmount, refundedAmount);
        LinkedInvoiceReference linkedInvoiceReference = linkedInvoiceReference(invoice);

        return new InvoiceSummaryResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getClient().getId(),
            invoice.getClient().getName(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getCurrency(),
            invoice.getStatus().name(),
            invoice.getDocumentType().name(),
            linkedInvoiceReference.originalInvoiceId(),
            linkedInvoiceReference.originalInvoiceNumber(),
            linkedInvoiceReference.linkedCreditNoteId(),
            linkedInvoiceReference.linkedCreditNoteNumber(),
            invoice.getSubtotal(),
            invoice.getTaxTotal(),
            invoice.getTotal(),
            paymentStatus.name(),
            paidAmount,
            invoicePaymentStateService.getOutstandingAmount(invoice, paidAmount),
            refundStatus.name(),
            refundableAmount,
            refundedAmount,
            remainingRefundAmount
        );
    }

    private InvoiceResponse toResponse(Invoice invoice, BigDecimal paidAmount) {
        InvoicePaymentStatus paymentStatus = invoicePaymentStateService.derivePaymentStatus(invoice, paidAmount);
        BigDecimal refundedAmount = invoiceRefundStateService.getRefundedAmount(invoice);
        BigDecimal refundableAmount = invoiceRefundStateService.getRefundableAmount(invoice);
        BigDecimal remainingRefundAmount = invoiceRefundStateService.getRemainingRefundAmount(invoice, refundedAmount);
        InvoiceRefundStatus refundStatus = invoiceRefundStateService.deriveRefundStatus(invoice, refundableAmount, refundedAmount);
        LinkedInvoiceReference linkedInvoiceReference = linkedInvoiceReference(invoice);

        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getClient().getId(),
            invoice.getClient().getName(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getCurrency(),
            invoice.getStatus().name(),
            invoice.getDocumentType().name(),
            linkedInvoiceReference.originalInvoiceId(),
            linkedInvoiceReference.originalInvoiceNumber(),
            linkedInvoiceReference.linkedCreditNoteId(),
            linkedInvoiceReference.linkedCreditNoteNumber(),
            invoice.getNote(),
            invoice.getSubtotal(),
            invoice.getTaxTotal(),
            invoice.getTotal(),
            paymentStatus.name(),
            paidAmount,
            invoicePaymentStateService.getOutstandingAmount(invoice, paidAmount),
            refundStatus.name(),
            refundableAmount,
            refundedAmount,
            remainingRefundAmount,
            invoice.getLines().stream().map(this::toLineResponse).toList()
        );
    }

    private LinkedInvoiceReference linkedInvoiceReference(Invoice invoice) {
        UUID originalInvoiceId = null;
        String originalInvoiceNumber = null;
        UUID linkedCreditNoteId = null;
        String linkedCreditNoteNumber = null;

        if (invoice.getOriginalInvoice() != null) {
            originalInvoiceId = invoice.getOriginalInvoice().getId();
            originalInvoiceNumber = invoice.getOriginalInvoice().getInvoiceNumber();
        } else {
            Invoice linkedCreditNote = invoiceRepository.findByOriginalInvoiceIdAndCompanyIdAndDeletedAtIsNull(
                invoice.getId(),
                invoice.getCompany().getId()
            ).orElse(null);

            if (linkedCreditNote != null) {
                linkedCreditNoteId = linkedCreditNote.getId();
                linkedCreditNoteNumber = linkedCreditNote.getInvoiceNumber();
            }
        }

        return new LinkedInvoiceReference(
            originalInvoiceId,
            originalInvoiceNumber,
            linkedCreditNoteId,
            linkedCreditNoteNumber
        );
    }

    private boolean matchesPaymentStatus(InvoiceSummaryResponse summary, InvoicePaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            return true;
        }

        return paymentStatus.name().equals(summary.paymentStatus());
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return new InvoiceLineResponse(
            line.getId(),
            line.getLineNumber(),
            line.getItem() != null ? line.getItem().getId() : null,
            line.getItemName(),
            line.getItemCode(),
            line.getUnit(),
            line.getQuantity(),
            line.getUnitPrice(),
            line.getTaxRate(),
            line.getLineSubtotal(),
            line.getLineTaxTotal(),
            line.getLineTotal()
        );
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Sort parseSort(String sort) {
        String requestedSort = sort == null || sort.isBlank() ? "issueDate,desc" : sort.trim();
        String[] parts = requestedSort.split(",", 2);
        String field = parts[0].trim();
        String mappedField = SORT_FIELD_MAPPING.get(field);

        if (mappedField == null) {
            throw new ApiBadRequestException("Invalid sort field.");
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new ApiBadRequestException("Invalid sort direction.");
            }
        }

        return Sort.by(direction, mappedField);
    }

    private InvoiceStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid invoice status.");
        }
    }

    private InvoicePaymentStatus parseOptionalPaymentStatus(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return null;
        }

        try {
            return InvoicePaymentStatus.valueOf(paymentStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid invoice payment status.");
        }
    }

    private String normalizeSearch(String search) {
        String normalized = normalizeOptional(search);
        return normalized == null ? "" : normalized;
    }

    private String normalizeCurrency(String currency) {
        String normalized = normalizeRequired(currency, "Required invoice field is missing.");
        return normalized.toUpperCase();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new ApiBadRequestException(message);
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateDates(LocalDate issueDate, LocalDate dueDate) {
        if (dueDate.isBefore(issueDate)) {
            throw new ApiBadRequestException("Due date must not be before issue date.");
        }
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            throw new ApiBadRequestException("Required invoice line field is missing.");
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBadRequestException("Invoice line quantity must be positive.");
        }

        return value;
    }

    private BigDecimal normalizeMoney(BigDecimal value, String message) {
        if (value == null) {
            throw new ApiBadRequestException(message);
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiBadRequestException("Invoice monetary values must not be negative.");
        }

        return money(value);
    }

    private BigDecimal normalizeTaxRate(BigDecimal value) {
        BigDecimal normalized = normalizeMoney(value, "Required invoice line field is missing.");

        if (normalized.compareTo(ONE_HUNDRED) > 0) {
            throw new ApiBadRequestException("Invalid tax rate.");
        }

        return normalized;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatInvoiceNumber(int year, int sequenceNumber) {
        return year + "-" + String.format("%04d", sequenceNumber);
    }

    private String formatCreditNoteNote(Invoice originalInvoice) {
        String prefix = "Credit note for invoice " + originalInvoice.getInvoiceNumber();
        String originalNote = normalizeOptional(originalInvoice.getNote());

        if (originalNote == null) {
            return prefix;
        }

        return prefix + " | " + originalNote;
    }

    private record InvoiceDraft(
        List<InvoiceLine> lines,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total
    ) {
    }

    private record LinkedInvoiceReference(
        UUID originalInvoiceId,
        String originalInvoiceNumber,
        UUID linkedCreditNoteId,
        String linkedCreditNoteNumber
    ) {
    }
}

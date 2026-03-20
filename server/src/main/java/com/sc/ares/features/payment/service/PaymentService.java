package com.sc.ares.features.payment.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.invoice.service.InvoiceAccessService;
import com.sc.ares.features.payment.dto.CreatePaymentRequest;
import com.sc.ares.features.payment.dto.PaymentResponse;
import com.sc.ares.features.payment.dto.PaymentSummaryResponse;
import com.sc.ares.features.payment.entity.Payment;
import com.sc.ares.features.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalDate MIN_FILTER_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_FILTER_DATE = LocalDate.of(2999, 12, 31);
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "paymentDate", "paymentDate",
        "amount", "amount",
        "createdAt", "createdAt"
    );

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAccessService invoiceAccessService;

    public PaymentService(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        InvoiceAccessService invoiceAccessService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceAccessService = invoiceAccessService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentSummaryResponse> listCompanyPayments(
        int page,
        int size,
        String sort,
        String search,
        LocalDate from,
        LocalDate to
    ) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        validateDateRange(from, to);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        List<PaymentSummaryResponse> items = paymentRepository.findAllByCompanyIdAndFilters(
            companyId,
            normalizeSearch(search),
            from != null,
            normalizeFromFilter(from),
            to != null,
            normalizeToFilter(to),
            parseSort(sort)
        ).stream()
            .map(this::toSummaryResponse)
            .toList();

        int fromIndex = Math.min(normalizedPage * normalizedSize, items.size());
        int toIndex = Math.min(fromIndex + normalizedSize, items.size());
        int totalItems = items.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / normalizedSize);

        return new PagedResponse<>(
            items.subList(fromIndex, toIndex),
            normalizedPage,
            normalizedSize,
            totalItems,
            totalPages
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        getRequiredInvoice(invoiceId, companyId);

        return paymentRepository.findAllByInvoice_IdAndCompany_IdOrderByPaymentDateDescCreatedAtDesc(invoiceId, companyId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public PaymentResponse createPayment(UUID invoiceId, CreatePaymentRequest request) {
        UUID companyId = invoiceAccessService.getRequiredWritableCompanyId();
        Invoice invoice = getRequiredInvoice(invoiceId, companyId);
        ensureIssued(invoice);

        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal currentPaidAmount = paidAmount(invoiceId, companyId);
        BigDecimal nextPaidAmount = currentPaidAmount.add(amount).setScale(2, RoundingMode.HALF_UP);

        if (nextPaidAmount.compareTo(invoice.getTotal()) > 0) {
            throw new BusinessConflictException("Payment amount exceeds outstanding invoice amount.");
        }

        Payment payment = Payment.create(
            invoice,
            request.paymentDate(),
            amount,
            normalizeOptional(request.note())
        );

        return toResponse(paymentRepository.save(payment));
    }

    private Invoice getRequiredInvoice(UUID invoiceId, UUID companyId) {
        return invoiceRepository.findByIdAndCompanyIdAndDeletedAtIsNull(invoiceId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }

    private void ensureIssued(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessConflictException("Payments can only be recorded for issued invoices.");
        }

        if (invoice.getDocumentType() != InvoiceDocumentType.INVOICE) {
            throw new BusinessConflictException("Payments cannot be recorded for credit notes.");
        }
    }

    private BigDecimal paidAmount(UUID invoiceId, UUID companyId) {
        return paymentRepository.sumAmountByInvoiceIdAndCompanyId(invoiceId, companyId)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ApiBadRequestException("Required payment field is missing.");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? "" : normalized;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ApiBadRequestException("Invalid payment date range.");
        }
    }

    private LocalDate normalizeFromFilter(LocalDate from) {
        return from != null ? from : MIN_FILTER_DATE;
    }

    private LocalDate normalizeToFilter(LocalDate to) {
        return to != null ? to : MAX_FILTER_DATE;
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
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "paymentDate");
        }

        String[] parts = sort.split(",", 2);
        String requestedField = parts[0].trim();
        String mappedField = SORT_FIELD_MAPPING.getOrDefault(requestedField, "paymentDate");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(direction, mappedField);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getNote()
        );
    }

    private PaymentSummaryResponse toSummaryResponse(Payment payment) {
        Invoice invoice = payment.getInvoice();

        return new PaymentSummaryResponse(
            payment.getId(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getNote(),
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getIssueDate(),
            invoice.getClient().getId(),
            invoice.getClient().getName(),
            invoice.getCurrency()
        );
    }
}

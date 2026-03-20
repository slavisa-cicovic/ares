package com.sc.ares.features.invoice.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceDocumentType;
import com.sc.ares.features.invoice.entity.InvoiceLine;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.payment.entity.Payment;
import com.sc.ares.features.payment.repository.PaymentRepository;
import com.sc.ares.features.refund.entity.CreditNoteRefund;
import com.sc.ares.features.refund.repository.CreditNoteRefundRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final Locale SERBIAN_LOCALE = Locale.forLanguageTag("sr-RS");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CreditNoteRefundRepository creditNoteRefundRepository;
    private final InvoicePaymentStateService invoicePaymentStateService;
    private final InvoiceRefundStateService invoiceRefundStateService;
    private final InvoiceAccessService invoiceAccessService;
    private final String invoiceTemplate;

    public InvoicePdfService(
        InvoiceRepository invoiceRepository,
        PaymentRepository paymentRepository,
        CreditNoteRefundRepository creditNoteRefundRepository,
        InvoicePaymentStateService invoicePaymentStateService,
        InvoiceRefundStateService invoiceRefundStateService,
        InvoiceAccessService invoiceAccessService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.creditNoteRefundRepository = creditNoteRefundRepository;
        this.invoicePaymentStateService = invoicePaymentStateService;
        this.invoiceRefundStateService = invoiceRefundStateService;
        this.invoiceAccessService = invoiceAccessService;
        this.invoiceTemplate = loadTemplate("templates/invoice/default.html");
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID invoiceId) {
        Invoice invoice = getRequiredInvoice(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessConflictException("Only issued documents can be exported as PDF.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(buildHtml(invoice), null);
        builder.toStream(outputStream);
        try {
            builder.run();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate invoice PDF.", exception);
        }

        return outputStream.toByteArray();
    }

    @Transactional(readOnly = true)
    public String getFilename(UUID invoiceId) {
        Invoice invoice = getRequiredInvoice(invoiceId);
        return buildFilename(invoice);
    }

    private Invoice getRequiredInvoice(UUID invoiceId) {
        UUID companyId = invoiceAccessService.getRequiredReadableCompanyId();
        return invoiceRepository.findByIdAndCompanyIdAndDeletedAtIsNull(invoiceId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }

    private String buildFilename(Invoice invoice) {
        String prefix = invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE ? "credit-note" : "invoice";
        return prefix + "-" + sanitizeFilename(invoice.getInvoiceNumber()) + ".pdf";
    }

    private String buildHtml(Invoice invoice) {
        UUID companyId = invoice.getCompany().getId();
        Company company = invoice.getCompany();
        Client client = invoice.getClient();
        BigDecimal paidAmount = invoicePaymentStateService.getPaidAmount(invoice);
        BigDecimal refundedAmount = invoiceRefundStateService.getRefundedAmount(invoice);
        List<Payment> payments = invoice.getDocumentType() == InvoiceDocumentType.INVOICE
            ? paymentRepository.findAllByInvoice_IdAndCompany_IdOrderByPaymentDateDescCreatedAtDesc(invoice.getId(), companyId)
            : List.of();
        List<CreditNoteRefund> refunds = invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE
            ? creditNoteRefundRepository.findAllByInvoice_IdAndCompany_IdOrderByRefundDateDescCreatedAtDesc(invoice.getId(), companyId)
            : List.of();
        Invoice linkedCreditNote = invoice.getDocumentType() == InvoiceDocumentType.INVOICE
            ? invoiceRepository.findByOriginalInvoiceIdAndCompanyIdAndDeletedAtIsNull(invoice.getId(), companyId).orElse(null)
            : null;

        String brandColor = company.getBrandColor() == null || company.getBrandColor().isBlank()
            ? "#c57f17"
            : company.getBrandColor();

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("brandColor", escapeHtml(brandColor));
        placeholders.put("brandInitial", escapeHtml(company.getName().substring(0, 1).toUpperCase(SERBIAN_LOCALE)));
        placeholders.put("documentTitle", invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE ? "Credit Note" : "Invoice");
        placeholders.put("companyName", escapeHtml(company.getName()));
        placeholders.put("documentRows", buildMetaRows(List.of(
            metaRow("Number", invoice.getInvoiceNumber()),
            metaRow("Issue Date", formatDate(invoice.getIssueDate())),
            metaRow("Due Date", formatDate(invoice.getDueDate())),
            metaRow("Currency", invoice.getCurrency())
        )));
        placeholders.put("issuerRows", buildMetaRows(List.of(
            metaRow("Name", company.getName()),
            metaRow("PIB", company.getPib()),
            metaRow("MB", company.getMb()),
            metaRow("Address", company.getAddress()),
            metaRow("Email", company.getEmail()),
            metaRow("Phone", company.getPhone()),
            metaRow("Bank Account", company.getBankAccount())
        )));
        placeholders.put("clientRows", buildMetaRows(List.of(
            metaRow("Name", client.getName()),
            metaRow("PIB", client.getPib()),
            metaRow("MB", client.getMb()),
            metaRow("Address", joinAddress(client)),
            metaRow("Email", client.getEmail()),
            metaRow("Phone", client.getPhone())
        )));
        placeholders.put("referenceSection", buildReferenceSection(invoice, linkedCreditNote));
        placeholders.put("lineRows", buildLineRows(invoice));
        placeholders.put("summaryRows", buildSummaryRows(invoice, paidAmount, refundedAmount));
        placeholders.put("noteSection", buildNoteSection(invoice));
        placeholders.put("paymentHistorySection", buildPaymentHistorySection(invoice, payments));
        placeholders.put("refundHistorySection", buildRefundHistorySection(invoice, refunds));

        return renderTemplate(invoiceTemplate, placeholders);
    }

    private String buildMetaRows(List<MetaRow> rows) {
        StringBuilder html = new StringBuilder();
        for (MetaRow row : rows) {
            if (row.value() == null || row.value().isBlank()) {
                continue;
            }

            html.append("<tr><td class=\"meta-label\">").append(escapeHtml(row.label())).append("</td><td>")
                .append(escapeHtml(row.value())).append("</td></tr>");
        }
        return html.toString();
    }

    private String buildReferenceSection(Invoice invoice, Invoice linkedCreditNote) {
        if (invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE && invoice.getOriginalInvoice() != null) {
            return "<div class=\"note\"><strong>Reference:</strong> This credit note references invoice "
                + escapeHtml(invoice.getOriginalInvoice().getInvoiceNumber()) + ".</div>";
        }

        if (linkedCreditNote != null) {
            return "<div class=\"note\"><strong>Reference:</strong> This invoice has credit note "
                + escapeHtml(linkedCreditNote.getInvoiceNumber()) + ".</div>";
        }

        return "";
    }

    private String buildLineRows(Invoice invoice) {
        StringBuilder html = new StringBuilder();
        for (InvoiceLine line : invoice.getLines()) {
            html.append("<tr><td>").append(line.getLineNumber()).append("</td><td>").append(escapeHtml(line.getItemName())).append("</td>");
            html.append("<td>").append(escapeHtml(orDash(line.getItemCode()))).append("</td><td>").append(escapeHtml(line.getUnit())).append("</td>");
            html.append("<td class=\"num\">").append(formatQuantity(line.getQuantity())).append("</td>");
            html.append("<td class=\"num\">").append(formatMoney(line.getUnitPrice(), invoice.getCurrency())).append("</td>");
            html.append("<td class=\"num\">").append(formatPercent(line.getTaxRate())).append("</td>");
            html.append("<td class=\"num\">").append(formatMoney(line.getLineTotal(), invoice.getCurrency())).append("</td></tr>");
        }
        return html.toString();
    }

    private String buildSummaryRows(Invoice invoice, BigDecimal paidAmount, BigDecimal refundedAmount) {
        StringBuilder html = new StringBuilder();
        appendSummaryRow(html, "Subtotal", formatMoney(invoice.getSubtotal(), invoice.getCurrency()), false);
        appendSummaryRow(html, "Tax", formatMoney(invoice.getTaxTotal(), invoice.getCurrency()), false);
        appendSummaryRow(html, "Total", formatMoney(invoice.getTotal(), invoice.getCurrency()), true);
        if (invoice.getDocumentType() == InvoiceDocumentType.INVOICE) {
            appendSummaryRow(html, "Paid", formatMoney(paidAmount, invoice.getCurrency()), false);
            appendSummaryRow(
                html,
                "Outstanding",
                formatMoney(invoicePaymentStateService.getOutstandingAmount(invoice, paidAmount), invoice.getCurrency()),
                false
            );
        } else {
            appendSummaryRow(html, "Refunded", formatMoney(refundedAmount, invoice.getCurrency()), false);
            appendSummaryRow(
                html,
                "Remaining Refund",
                formatMoney(invoiceRefundStateService.getRemainingRefundAmount(invoice, refundedAmount), invoice.getCurrency()),
                false
            );
        }
        return html.toString();
    }

    private String buildNoteSection(Invoice invoice) {
        if (invoice.getNote() == null || invoice.getNote().isBlank()) {
            return "";
        }

        return "<div class=\"note\"><strong>Note:</strong><br />" + escapeHtml(invoice.getNote()) + "</div>";
    }

    private String buildPaymentHistorySection(Invoice invoice, List<Payment> payments) {
        if (payments.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"spacer\"></div><p class=\"section-title\">Payment History</p>");
        html.append("<table class=\"history-table\"><thead><tr><th>Date</th><th>Note</th><th class=\"num\">Amount</th></tr></thead><tbody>");
        for (Payment payment : payments) {
            html.append("<tr><td>").append(formatDate(payment.getPaymentDate())).append("</td><td>")
                .append(escapeHtml(orDash(payment.getNote()))).append("</td><td class=\"num\">")
                .append(formatMoney(payment.getAmount(), invoice.getCurrency())).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildRefundHistorySection(Invoice invoice, List<CreditNoteRefund> refunds) {
        if (refunds.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"spacer\"></div><p class=\"section-title\">Refund History</p>");
        html.append("<table class=\"history-table\"><thead><tr><th>Date</th><th>Note</th><th class=\"num\">Amount</th></tr></thead><tbody>");
        for (CreditNoteRefund refund : refunds) {
            html.append("<tr><td>").append(formatDate(refund.getRefundDate())).append("</td><td>")
                .append(escapeHtml(orDash(refund.getNote()))).append("</td><td class=\"num\">")
                .append(formatMoney(refund.getAmount(), invoice.getCurrency())).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String renderTemplate(String template, Map<String, String> placeholders) {
        String rendered = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private void appendSummaryRow(StringBuilder html, String label, String value, boolean total) {
        html.append("<tr");
        if (total) {
            html.append(" class=\"summary-total\"");
        }
        html.append("><td class=\"label\">").append(escapeHtml(label)).append("</td><td class=\"value\">")
            .append(escapeHtml(value)).append("</td></tr>");
    }

    private String loadTemplate(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load invoice PDF template.", exception);
        }
    }

    private MetaRow metaRow(String label, String value) {
        return new MetaRow(label, value);
    }

    private String joinAddress(Client client) {
        StringBuilder address = new StringBuilder();
        appendPart(address, client.getAddress());
        appendPart(address, client.getPostalCode());
        appendPart(address, client.getCity());
        appendPart(address, client.getCountry());
        return address.toString();
    }

    private void appendPart(StringBuilder target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!target.isEmpty()) {
            target.append(", ");
        }

        target.append(value);
    }

    private String formatDate(LocalDate value) {
        return value.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal value, String currency) {
        NumberFormat formatter = NumberFormat.getNumberInstance(SERBIAN_LOCALE);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(value) + " " + currency;
    }

    private String formatQuantity(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(SERBIAN_LOCALE);
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
        return formatter.format(value);
    }

    private String formatPercent(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(SERBIAN_LOCALE);
        formatter.setMinimumFractionDigits(value.stripTrailingZeros().scale() <= 0 ? 0 : 2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(value) + " %";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String sanitizeFilename(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record MetaRow(String label, String value) {
    }
}

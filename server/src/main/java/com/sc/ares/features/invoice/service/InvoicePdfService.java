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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

        StringBuilder html = new StringBuilder();
        html.append("""
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
  <meta charset="utf-8" />
  <style>
    @page { size: A4; margin: 22mm 16mm 20mm 16mm; }
    body { font-family: Arial, Helvetica, sans-serif; color: #1f2937; font-size: 12px; }
    .topbar { height: 12px; border-radius: 999px; margin-bottom: 18px; }
    .header-table, .meta-table, .summary-table, .history-table, .line-table { width: 100%; border-collapse: collapse; }
    .header-table td { vertical-align: top; }
    .brand-mark { width: 58px; height: 58px; border-radius: 16px; color: #fff; font-size: 28px; font-weight: 700; text-align: center; line-height: 58px; }
    .doc-title { font-size: 28px; font-weight: 700; margin: 0 0 6px 0; }
    .doc-subtitle { color: #6b7280; font-size: 12px; margin: 0; }
    .section-title { font-size: 11px; font-weight: 700; color: #6b7280; margin: 0 0 8px 0; text-transform: uppercase; letter-spacing: 0.08em; }
    .card { border: 1px solid #e5e7eb; border-radius: 14px; padding: 14px; }
    .meta-table td { padding: 4px 0; }
    .meta-label { color: #6b7280; width: 34%; }
    .line-table { margin-top: 18px; }
    .line-table th { background: #f8fafc; color: #475569; font-size: 11px; text-align: left; padding: 10px 8px; border-bottom: 1px solid #e2e8f0; }
    .line-table td { padding: 10px 8px; border-bottom: 1px solid #e5e7eb; }
    .line-table .num, .summary-table .value, .history-table .num { text-align: right; white-space: nowrap; }
    .summary-wrap { margin-top: 18px; }
    .summary-table td { padding: 6px 0; }
    .summary-table .label { color: #6b7280; }
    .summary-total td { font-size: 15px; font-weight: 700; }
    .note { margin-top: 18px; padding: 14px; border-radius: 14px; background: #f8fafc; }
    .history-table { margin-top: 10px; }
    .history-table th, .history-table td { padding: 8px 6px; border-bottom: 1px solid #e5e7eb; text-align: left; }
    .spacer { height: 18px; }
  </style>
</head>
<body>
""");

        html.append("<div class=\"topbar\" style=\"background: ").append(escapeHtml(brandColor)).append(";\"></div>");
        html.append("<table class=\"header-table\"><tr>");
        html.append("<td style=\"width: 62%;\"><table><tr><td style=\"padding-right: 16px;\">");
        html.append("<div class=\"brand-mark\" style=\"background: ").append(escapeHtml(brandColor)).append(";\">")
            .append(escapeHtml(company.getName().substring(0, 1).toUpperCase(SERBIAN_LOCALE))).append("</div>");
        html.append("</td><td><h1 class=\"doc-title\">")
            .append(invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE ? "Credit Note" : "Invoice")
            .append("</h1><p class=\"doc-subtitle\">").append(escapeHtml(company.getName())).append("</p></td></tr></table></td>");
        html.append("<td style=\"width: 38%;\">");
        appendMetaCard(html, "Document", List.of(
            metaRow("Number", invoice.getInvoiceNumber()),
            metaRow("Issue Date", formatDate(invoice.getIssueDate())),
            metaRow("Due Date", formatDate(invoice.getDueDate())),
            metaRow("Currency", invoice.getCurrency())
        ));
        html.append("</td></tr></table>");

        html.append("<div class=\"spacer\"></div><table class=\"header-table\"><tr>");
        html.append("<td style=\"width: 50%; padding-right: 8px;\">");
        appendMetaCard(html, "Issuer", List.of(
            metaRow("Name", company.getName()),
            metaRow("PIB", company.getPib()),
            metaRow("MB", company.getMb()),
            metaRow("Address", company.getAddress()),
            metaRow("Email", company.getEmail()),
            metaRow("Phone", company.getPhone()),
            metaRow("Bank Account", company.getBankAccount())
        ));
        html.append("</td><td style=\"width: 50%; padding-left: 8px;\">");
        appendMetaCard(html, "Client", List.of(
            metaRow("Name", client.getName()),
            metaRow("PIB", client.getPib()),
            metaRow("MB", client.getMb()),
            metaRow("Address", joinAddress(client)),
            metaRow("Email", client.getEmail()),
            metaRow("Phone", client.getPhone())
        ));
        html.append("</td></tr></table>");

        if (invoice.getDocumentType() == InvoiceDocumentType.CREDIT_NOTE && invoice.getOriginalInvoice() != null) {
            html.append("<div class=\"note\"><strong>Reference:</strong> This credit note references invoice ")
                .append(escapeHtml(invoice.getOriginalInvoice().getInvoiceNumber())).append(".</div>");
        } else if (linkedCreditNote != null) {
            html.append("<div class=\"note\"><strong>Reference:</strong> This invoice has credit note ")
                .append(escapeHtml(linkedCreditNote.getInvoiceNumber())).append(".</div>");
        }

        html.append("<table class=\"line-table\"><thead><tr>");
        html.append("<th style=\"width: 6%;\">#</th><th style=\"width: 31%;\">Item</th><th style=\"width: 11%;\">Code</th>");
        html.append("<th style=\"width: 10%;\">Unit</th><th class=\"num\" style=\"width: 11%;\">Quantity</th>");
        html.append("<th class=\"num\" style=\"width: 13%;\">Price</th><th class=\"num\" style=\"width: 8%;\">Tax</th>");
        html.append("<th class=\"num\" style=\"width: 10%;\">Total</th></tr></thead><tbody>");

        for (InvoiceLine line : invoice.getLines()) {
            html.append("<tr><td>").append(line.getLineNumber()).append("</td><td>").append(escapeHtml(line.getItemName())).append("</td>");
            html.append("<td>").append(escapeHtml(orDash(line.getItemCode()))).append("</td><td>").append(escapeHtml(line.getUnit())).append("</td>");
            html.append("<td class=\"num\">").append(formatQuantity(line.getQuantity())).append("</td>");
            html.append("<td class=\"num\">").append(formatMoney(line.getUnitPrice(), invoice.getCurrency())).append("</td>");
            html.append("<td class=\"num\">").append(formatPercent(line.getTaxRate())).append("</td>");
            html.append("<td class=\"num\">").append(formatMoney(line.getLineTotal(), invoice.getCurrency())).append("</td></tr>");
        }

        html.append("</tbody></table>");

        html.append("<div class=\"summary-wrap\"><table class=\"summary-table\">");
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
        html.append("</table></div>");

        if (invoice.getNote() != null && !invoice.getNote().isBlank()) {
            html.append("<div class=\"note\"><strong>Note:</strong><br />")
                .append(escapeHtml(invoice.getNote())).append("</div>");
        }

        if (!payments.isEmpty()) {
            html.append("<div class=\"spacer\"></div><p class=\"section-title\">Payment History</p>");
            html.append("<table class=\"history-table\"><thead><tr><th>Date</th><th>Note</th><th class=\"num\">Amount</th></tr></thead><tbody>");
            for (Payment payment : payments) {
                html.append("<tr><td>").append(formatDate(payment.getPaymentDate())).append("</td><td>")
                    .append(escapeHtml(orDash(payment.getNote()))).append("</td><td class=\"num\">")
                    .append(formatMoney(payment.getAmount(), invoice.getCurrency())).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }

        if (!refunds.isEmpty()) {
            html.append("<div class=\"spacer\"></div><p class=\"section-title\">Refund History</p>");
            html.append("<table class=\"history-table\"><thead><tr><th>Date</th><th>Note</th><th class=\"num\">Amount</th></tr></thead><tbody>");
            for (CreditNoteRefund refund : refunds) {
                html.append("<tr><td>").append(formatDate(refund.getRefundDate())).append("</td><td>")
                    .append(escapeHtml(orDash(refund.getNote()))).append("</td><td class=\"num\">")
                    .append(formatMoney(refund.getAmount(), invoice.getCurrency())).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private void appendMetaCard(StringBuilder html, String title, List<MetaRow> rows) {
        html.append("<div class=\"card\"><p class=\"section-title\">").append(escapeHtml(title)).append("</p><table class=\"meta-table\">");
        for (MetaRow row : rows) {
            if (row.value() == null || row.value().isBlank()) {
                continue;
            }

            html.append("<tr><td class=\"meta-label\">").append(escapeHtml(row.label())).append("</td><td>")
                .append(escapeHtml(row.value())).append("</td></tr>");
        }
        html.append("</table></div>");
    }

    private void appendSummaryRow(StringBuilder html, String label, String value, boolean total) {
        html.append("<tr");
        if (total) {
            html.append(" class=\"summary-total\"");
        }
        html.append("><td class=\"label\">").append(escapeHtml(label)).append("</td><td class=\"value\">")
            .append(escapeHtml(value)).append("</td></tr>");
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

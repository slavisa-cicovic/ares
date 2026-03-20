package com.sc.ares.features.invoice.controller;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.invoice.dto.InvoiceResponse;
import com.sc.ares.features.invoice.dto.InvoiceSummaryResponse;
import com.sc.ares.features.invoice.dto.UpsertInvoiceRequest;
import com.sc.ares.features.invoice.service.InvoicePdfService;
import com.sc.ares.features.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping
    public PagedResponse<InvoiceSummaryResponse> listInvoices(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "issueDate,desc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        return invoiceService.listInvoices(page, size, sort, status, paymentStatus, search, from, to);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable UUID invoiceId) {
        return invoiceService.getInvoice(invoiceId);
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable UUID invoiceId) {
        String filename = invoicePdfService.getFilename(invoiceId);
        byte[] pdf = invoicePdfService.generatePdf(invoiceId);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
            .body(pdf);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody UpsertInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @PostMapping("/{invoiceId}/issue")
    public InvoiceResponse issueInvoice(@PathVariable UUID invoiceId) {
        return invoiceService.issueInvoice(invoiceId);
    }

    @PostMapping("/{invoiceId}/credit-note")
    public InvoiceResponse createCreditNote(@PathVariable UUID invoiceId) {
        return invoiceService.createCreditNote(invoiceId);
    }

    @PutMapping("/{invoiceId}")
    public InvoiceResponse updateInvoice(@PathVariable UUID invoiceId, @Valid @RequestBody UpsertInvoiceRequest request) {
        return invoiceService.updateInvoice(invoiceId, request);
    }
}

package com.sc.ares.features.invoice.controller;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.invoice.dto.InvoiceResponse;
import com.sc.ares.features.invoice.dto.InvoiceSummaryResponse;
import com.sc.ares.features.invoice.dto.UpsertInvoiceRequest;
import com.sc.ares.features.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public PagedResponse<InvoiceSummaryResponse> listInvoices(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "issueDate,desc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        return invoiceService.listInvoices(page, size, sort, status, search);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable UUID invoiceId) {
        return invoiceService.getInvoice(invoiceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody UpsertInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @PutMapping("/{invoiceId}")
    public InvoiceResponse updateInvoice(@PathVariable UUID invoiceId, @Valid @RequestBody UpsertInvoiceRequest request) {
        return invoiceService.updateInvoice(invoiceId, request);
    }
}

package com.sc.ares.features.refund.controller;

import com.sc.ares.features.refund.dto.CreateRefundRequest;
import com.sc.ares.features.refund.dto.RefundResponse;
import com.sc.ares.features.refund.service.RefundService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices/{invoiceId}/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public List<RefundResponse> listRefunds(@PathVariable UUID invoiceId) {
        return refundService.listRefunds(invoiceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse createRefund(@PathVariable UUID invoiceId, @Valid @RequestBody CreateRefundRequest request) {
        return refundService.createRefund(invoiceId, request);
    }
}

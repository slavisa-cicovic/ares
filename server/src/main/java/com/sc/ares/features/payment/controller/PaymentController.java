package com.sc.ares.features.payment.controller;

import com.sc.ares.features.payment.dto.CreatePaymentRequest;
import com.sc.ares.features.payment.dto.PaymentResponse;
import com.sc.ares.features.payment.service.PaymentService;
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
@RequestMapping("/invoices/{invoiceId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> listPayments(@PathVariable UUID invoiceId) {
        return paymentService.listPayments(invoiceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@PathVariable UUID invoiceId, @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(invoiceId, request);
    }
}

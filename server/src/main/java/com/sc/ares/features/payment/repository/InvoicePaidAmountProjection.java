package com.sc.ares.features.payment.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvoicePaidAmountProjection {

    UUID getInvoiceId();

    BigDecimal getPaidAmount();
}

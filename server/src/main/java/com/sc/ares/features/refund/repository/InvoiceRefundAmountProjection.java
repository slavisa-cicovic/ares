package com.sc.ares.features.refund.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvoiceRefundAmountProjection {
    UUID getInvoiceId();
    BigDecimal getRefundedAmount();
}

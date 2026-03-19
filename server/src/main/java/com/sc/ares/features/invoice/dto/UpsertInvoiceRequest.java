package com.sc.ares.features.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertInvoiceRequest(
    @NotNull
    UUID clientId,
    @NotNull
    LocalDate issueDate,
    @NotNull
    LocalDate dueDate,
    @NotBlank
    @Size(min = 3, max = 3)
    String currency,
    @Size(max = 1000)
    String note,
    @NotEmpty
    List<@Valid UpsertInvoiceLineRequest> lines
) {
}

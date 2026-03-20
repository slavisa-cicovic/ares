# PLAN_13_reports-foundation.md

## Project
Ares

## Goal
Introduce the first reporting/dashboard backend foundation so the application can expose meaningful business summaries per company.

---

## Summary

The backend already supports:

- tenant-safe invoices
- invoice lifecycle
- payment recording
- derived payment status

The next step is to expose operational summaries that answer basic business questions such as:

- how much was invoiced
- how much was paid
- how much is still open
- how many invoices are unpaid, partially paid, or paid

This phase should introduce:

- reporting read models
- dashboard-style summary endpoint(s)
- date-range based summary filtering

This is the phase where the backend starts serving management insight, not just transactional CRUD.

---

## Scope

This plan covers:

- first reporting summary endpoint
- invoice and payment aggregation per company
- date range filtering
- payment-status-based counts and amounts
- overdue summary direction if it fits cleanly
- integration tests for reporting behavior

This plan does not cover:

- chart-specific frontend formatting
- export to CSV/PDF
- advanced analytics
- tax-specific regulatory reports
- accountant-grade ledger reports

---

## Why This Comes Next

The backend now has enough operational data for useful summaries.

Without reporting, the system can store and edit data, but it still cannot answer the simplest management questions efficiently.

This phase should turn:

- invoices
- payments
- payment status

into:

- business overview
- receivables visibility
- dashboard-ready aggregates

---

## Core Decisions

This phase should lock the following:

- reporting is tenant-scoped
- reporting is backend-derived, not client-computed
- summary values are based on persisted invoices and payments
- first reporting phase should stay compact and practical

Recommended v1 direction:

- one summary endpoint first
- stable JSON response with simple numeric fields
- date filters applied at the backend

Reason:

- this gives immediate value without overdesigning reporting architecture too early

---

## Endpoint Direction

Recommended first endpoint:

- `GET /reports/summary`

Suggested query parameters:

- `from` optional
- `to` optional

Recommended response direction:

- summary is always scoped to authenticated company
- `company-admin` and regular `user` may read it
- response is dashboard-friendly and does not require client aggregation

---

## Summary Content Direction

Recommended fields in the first summary response:

- `invoiceCount`
- `totalInvoicedAmount`
- `totalPaidAmount`
- `totalOutstandingAmount`
- `unpaidInvoiceCount`
- `partiallyPaidInvoiceCount`
- `paidInvoiceCount`

Recommended optional additions if easy to support now:

- `overdueInvoiceCount`
- `overdueOutstandingAmount`

Reason:

- these fields answer the most immediate business questions
- they map directly to dashboard cards and summary widgets

---

## Date Range Direction

Recommended first filtering rule:

- report date range is based on invoice `issueDate`

Suggested behavior:

- if `from` is absent, no lower bound
- if `to` is absent, no upper bound
- if both are absent, summary covers all company data
- invalid range where `to < from` returns `400`

Reason:

- `issueDate` is the cleanest first reporting anchor
- payment-date-specific reports can come later

---

## Payment Status Aggregation Direction

The summary should use the same derived payment status model introduced earlier:

- `UNPAID`
- `PARTIALLY_PAID`
- `PAID`

Required rule:

- reporting must not duplicate inconsistent payment-state logic
- payment status derivation should be reused or centralized

This is important because reporting and invoice lists must always agree.

---

## Overdue Direction

Recommended v1 overdue rule:

- invoice is overdue when:
  - status is `ISSUED`
  - outstanding amount is greater than zero
  - `dueDate` is before current date

If overdue support is added now, it should remain derived, not persisted.

If it complicates the first iteration too much, it can be deferred to the next plan.

My recommendation:

- include overdue count and overdue outstanding amount if the implementation stays clean

---

## Tenant Enforcement Rules

Required direction:

- all report data is scoped to authenticated company
- cross-tenant report access is impossible
- no platform-wide reporting is included in this phase

This keeps reporting aligned with the rest of the multi-tenant backend.

---

## Authorization Direction

Recommended direction:

- `company-admin` can access report summary
- regular `user` can access report summary
- `super-admin` is not automatically included unless explicitly designed later

Reason:

- summary is a read concern for the tenant
- it does not change data

---

## Implementation Direction

Recommended pragmatic direction:

- build reporting as a dedicated `reports` feature module
- keep aggregation logic in a service, not in the controller
- use repository queries where they are simple and safe
- allow some application-side aggregation if it keeps correctness clearer

This phase should optimize for:

- correctness
- maintainability
- consistent tenant behavior

Not for:

- maximum query cleverness

---

## Persistence Direction

This phase should not require new tables.

Recommended direction:

- derive report data from existing:
  - invoices
  - payments
  - derived payment state

Possible future evolution:

- materialized reporting tables
- scheduled aggregates
- warehouse/export flows

But none of that should be introduced now.

---

## Validation Direction

This phase should validate:

- `from` and `to` are valid dates
- `to` is not before `from`

Suggested response:

- `400 Bad Request` with clear message for invalid range

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can read report summary
- regular user can read report summary
- cross-tenant isolation remains intact
- summary totals match seeded invoices and payments
- payment-status counts are correct
- date range limits the summary correctly
- invalid date range returns `400`
- overdue values, if included, are correct

---

## Deliverables

- first reporting summary endpoint exists
- summary response contains core invoicing/payment aggregates
- date range filtering works
- integration tests cover summary correctness and tenant behavior

---

## Acceptance Criteria

- tenant users can fetch a useful company-level summary
- reported invoiced, paid, and outstanding amounts are correct
- payment-status counts match invoice payment state rules
- date filtering works correctly
- report data is fully tenant-safe

---

## Risks

- duplicated aggregation logic could drift from invoice read models
- premature query optimization could make reporting harder to maintain
- mixing too many report variants into the first summary phase would dilute focus

---

## Next Step After This Plan

After this plan, the likely next step is one of:

- overdue-focused reporting
- date-series/chart reporting
- export/report download features

The choice depends on what the first client app needs most.

---

## Assumptions

- invoice and payment status logic already exists and is trusted
- first reporting use case is dashboard-style overview
- tenant-level reporting is more important than platform-wide reporting right now

---

## Notes

- This phase should stay business-focused and concise.
- The goal is to expose immediately useful summary information, not to build a full BI layer.

# PLAN_12_invoice-payment-status.md

## Project
Ares

## Goal
Expose invoice payment state in a form that is directly usable by clients and ready for later reporting.

---

## Summary

The backend already supports:

- draft and issued invoices
- immutable issued invoices
- payment recording for issued invoices
- backend calculation of `paidAmount` and `outstandingAmount`

The next step is to make invoice payment state explicit.

This phase should introduce:

- invoice payment status as a derived business field
- invoice list filtering by payment state
- invoice-level summary values that are ready for dashboard and report use

This is the phase where invoice collections become operationally useful instead of only technically complete.

---

## Scope

This plan covers:

- derived invoice payment status
- invoice response updates
- invoice list filtering by payment status
- summary direction for invoice collections
- integration tests for payment-state behavior

This plan does not cover:

- PDF generation
- report screens
- dashboard endpoints
- cancellation/storno
- overdue reminders

---

## Why This Comes Next

The backend now knows:

- invoice total
- paid amount
- outstanding amount

But clients still need to infer business meaning manually.

That is unnecessary and error-prone.

The backend should now expose clear payment-state semantics such as:

- unpaid
- partially paid
- paid

This is the smallest next step before real reporting.

---

## Core Decisions

This phase should lock the following:

- invoice payment status is derived, not stored as a separate persisted field
- payment status depends on current invoice total and active payments
- payment status is returned by invoice APIs
- invoice lists can be filtered by payment status

Recommended v1 direction:

- keep payment status derived in application logic
- do not add another persisted status column yet

Reason:

- the state is already computable from existing data
- storing it separately would create consistency risk too early

---

## Payment Status Direction

Recommended derived payment statuses:

- `UNPAID`
- `PARTIALLY_PAID`
- `PAID`

Rules:

- `UNPAID` when `paidAmount = 0`
- `PARTIALLY_PAID` when `paidAmount > 0` and `paidAmount < total`
- `PAID` when `paidAmount = total`

Recommended behavior for draft invoices:

- draft invoices should still be returned with a payment status field
- `DRAFT` invoices should behave as `UNPAID` for now

Reason:

- this keeps the response shape uniform
- clients do not need special-case parsing

Alternative would be a separate `paymentStatus = null` for drafts, but that adds more client branching with little value.

---

## Response Direction

This phase should extend invoice responses with:

- `paymentStatus`

That applies to:

- invoice detail response
- invoice summary/list response

The response should continue to expose:

- `total`
- `paidAmount`
- `outstandingAmount`

This gives clients both:

- machine-friendly business status
- raw numeric amounts

---

## Filtering Direction

This phase should extend `GET /invoices` with:

- optional `paymentStatus` filter

Supported values:

- `UNPAID`
- `PARTIALLY_PAID`
- `PAID`

Recommended behavior:

- filter is applied after tenant scoping
- invalid payment status returns `400`

Implementation direction:

- filtering may be done in application logic first if that keeps complexity reasonable
- if needed later, this can be optimized into query-level aggregation

Reason:

- correctness matters more than query sophistication in the first version

---

## Summary Direction

This phase should also prepare the backend for reporting by defining summary values that can later be reused.

Recommended summary concepts:

- invoice count by payment status
- total invoiced amount
- total paid amount
- total outstanding amount

My recommendation:

- do not add a dedicated summary endpoint in this phase unless it comes almost for free
- first make sure invoice responses and list filters are correct

That keeps this phase focused.

---

## Tenant Enforcement Rules

Required direction:

- payment status derivation remains tenant-safe because it is based on tenant-safe invoice and payment data
- invoice list filtering by payment status must still only operate on the authenticated company scope
- cross-tenant visibility remains impossible

---

## Authorization Direction

Recommended direction:

- `company-admin` can read invoice payment state and use filters
- regular `user` can read invoice payment state and use filters
- write permissions remain unchanged from earlier phases

This phase is a read-model improvement, not a write-permission change.

---

## Persistence Direction

This phase should not require a new table.

Recommended direction:

- keep payment status derived from existing invoice and payment data
- avoid a persisted `payment_status` column for now

Possible future optimization:

- materialized summaries or denormalized reporting data if query cost becomes meaningful

But that is not needed yet.

---

## Validation Direction

This phase should validate:

- payment status filter value is supported
- derived payment status is internally consistent with `paidAmount` and `total`

Recommended pragmatic direction:

- keep calculation rules centralized in one service/helper path
- avoid duplicating derivation logic across controllers or DTO factories

---

## Testing Expectations

This phase should add integration tests for:

- issued invoice with no payments returns `UNPAID`
- issued invoice with partial payments returns `PARTIALLY_PAID`
- fully paid invoice returns `PAID`
- draft invoice returns consistent default payment state
- invoice list can filter by `UNPAID`
- invoice list can filter by `PARTIALLY_PAID`
- invoice list can filter by `PAID`
- invalid payment status filter returns `400`
- cross-tenant list behavior remains correct

---

## Deliverables

- invoice responses include `paymentStatus`
- invoice list supports `paymentStatus` filter
- payment state derivation is centralized and tested
- integration tests cover payment state transitions and filters

---

## Acceptance Criteria

- invoice detail clearly exposes payment status
- invoice list can be filtered by payment status
- payment status always matches paid and outstanding amounts
- tenant isolation remains intact
- clients no longer need to derive payment state manually

---

## Risks

- duplicating payment-state calculation in multiple places would drift over time
- mixing lifecycle status and payment status semantics would confuse the model
- premature SQL optimization could overcomplicate a simple derived-state phase

---

## Next Step After This Plan

After this plan, the likely next step is:

- reporting foundation

That phase can introduce:

- dashboard summaries
- overdue invoice counts
- receivables totals
- date-range based reporting

---

## Assumptions

- payment records remain append-only in v1
- invoice totals remain backend-controlled
- payment state is derived from current persisted payment rows

---

## Notes

- This phase is intentionally about read-model clarity.
- The key value is making invoice payment meaning explicit and reusable.

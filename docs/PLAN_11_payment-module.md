# PLAN_11_payment-module.md

## Project
Ares

## Goal
Introduce payment recording for issued invoices so the backend can track paid and outstanding amounts per invoice.

---

## Summary

The backend already supports:

- tenant-safe invoice aggregate
- draft invoice creation and editing
- invoice issuing
- immutable issued invoices

The next business step is to record money received against issued invoices.

This phase should introduce:

- payment records linked to invoices
- tenant-safe payment endpoints
- backend-owned paid amount calculation
- outstanding amount calculation

This is the phase where invoices stop being only issued documents and start participating in receivables tracking.

---

## Scope

This plan covers:

- payment entity and persistence
- payment list endpoint for an invoice
- payment create endpoint for an invoice
- optional payment update/delete behavior if kept simple and safe
- backend calculation of `paidAmount` and `outstandingAmount`
- integration tests for payment flows

This plan does not cover:

- payment provider integrations
- bank statement import
- automatic reconciliation
- refunds
- storno or cancellation
- general ledger/accounting export

---

## Why This Comes Next

The backend already has:

- invoice issuing
- immutable issued invoice records
- tenant-safe client and item modules

Without payments, the system cannot answer practical questions such as:

- how much has been paid
- how much remains open
- whether an invoice is fully settled

Payments are the smallest meaningful next step after invoice lifecycle.

---

## Core Decisions

This phase should lock the following:

- payments can be recorded only for `ISSUED` invoices
- payments always belong to exactly one invoice
- payment ownership is tenant-safe through invoice ownership
- paid and outstanding amounts are calculated on the backend
- cross-tenant payment access remains impossible

Recommended v1 direction:

- payments are append-only business records by default
- if edit/delete is allowed, it must stay restricted to simple admin correction scenarios

---

## Domain Direction

Suggested payment fields in v1:

- `id`
- `invoice_id`
- `company_id`
- `payment_date`
- `amount`
- `note`
- `created_at`
- `updated_at`
- `deleted_at` if soft delete is chosen

Recommended behavior:

- payment amount is positive
- payment date is required
- payment note is optional
- currency is inherited from invoice in v1

Reason:

- a single-currency per invoice approach keeps the first payment phase simple
- multi-currency payment handling would add complexity too early

---

## Invoice Relationship Rules

Required rules:

- a payment belongs to one invoice
- a payment belongs to the same company as its invoice
- payment endpoints must resolve invoice by `id + company_id`
- a payment cannot be created for an invoice from another company
- a payment cannot be attached to a `DRAFT` invoice

Recommended response direction:

- cross-tenant invoice or payment access returns `404`
- payment creation for non-issued invoice returns `409`

---

## Amount Calculation Direction

This phase should introduce two invoice-level derived values:

- `paidAmount`
- `outstandingAmount`

Rules:

- `paidAmount` = sum of active payments for the invoice
- `outstandingAmount` = `invoice.total - paidAmount`
- calculations must be backend-owned
- client input must not override these values

Recommended v1 direction:

- do not allow negative outstanding amount
- reject payment creation if it would exceed invoice total

Reason:

- overpayment handling is a real business case, but it complicates the first payment phase
- rejecting overpayments keeps invoice state simpler for now

---

## Endpoint Direction

Recommended endpoints:

- `GET /invoices/{id}/payments`
- `POST /invoices/{id}/payments`

Optional in this phase only if kept simple:

- `PUT /payments/{id}`
- `DELETE /payments/{id}`

My recommendation:

- implement list and create first
- postpone update/delete unless they are truly needed now

Reason:

- append-only payments are safer
- correction workflows can come later if needed

---

## Authorization Direction

Recommended direction:

- `company-admin` can create payments for own company invoices
- `company-admin` can list payments for own company invoices
- regular `user` can read payments for own company invoices
- regular `user` cannot create payments

This keeps payment write behavior aligned with invoice write rules.

---

## Tenant Enforcement Rules

Required direction:

- every payment belongs to one company through invoice ownership
- payment list and create endpoints are tenant-scoped
- payment lookup must never rely on payment id alone without tenant-safe join/scoping
- tenant context continues to come only from authenticated backend context

Implementation direction:

- for invoice-scoped routes, lookup invoice by `id + company_id`
- for payment-specific routes in future, lookup payment through tenant-safe repository logic

---

## API Response Direction

Recommended response behavior:

- list payments returns payments ordered by `paymentDate desc`, then `createdAt desc`
- create payment returns created payment response
- invoice response should expose `paidAmount` and `outstandingAmount`

Suggested response codes:

- `200` for successful list
- `201` for successful create
- `404` for missing or cross-tenant invoice
- `409` for invalid invoice state or overpayment attempt

---

## Persistence Direction

This phase should add:

- `payments` table

Recommended fields:

- `id uuid`
- `invoice_id uuid not null`
- `company_id uuid not null`
- `payment_date date not null`
- `amount numeric(...) not null`
- `note varchar(...) null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- optional `deleted_at timestamptz null`

Recommended indexes:

- index on `invoice_id`
- index on `company_id`
- index on `(company_id, payment_date)`

Reason for storing `company_id` directly:

- it makes tenant-safe filtering simpler
- it supports reporting queries later without always joining through invoices

---

## Validation Direction

This phase should validate:

- invoice exists in authenticated tenant
- invoice status is `ISSUED`
- amount is positive
- payment date is present
- total paid amount after insert does not exceed invoice total

Recommended pragmatic direction:

- do not add complex accounting validation yet
- focus on tenant safety, state safety, and amount integrity

---

## Read Model Direction

This phase should update invoice read responses so clients can display payment state without extra manual calculation.

Recommended additions to invoice response:

- `paidAmount`
- `outstandingAmount`

Optional future addition, but not required now:

- invoice payment status such as `UNPAID`, `PARTIALLY_PAID`, `PAID`

My recommendation:

- add amounts now
- introduce payment status enum in the next phase if needed

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can create payment for own issued invoice
- regular user cannot create payment
- regular user can list own-tenant payments if read access is allowed
- cross-tenant payment list/create returns `404`
- payment cannot be created for `DRAFT` invoice
- payment cannot exceed outstanding amount
- payment list returns only invoice payments from same tenant
- invoice response shows updated `paidAmount` and `outstandingAmount`

---

## Deliverables

- payment persistence exists
- invoice payment list endpoint exists
- invoice payment create endpoint exists
- backend calculates paid and outstanding amounts
- integration tests cover allowed and denied payment scenarios

---

## Acceptance Criteria

- company-admin can register a payment for an issued invoice in own company
- payments cannot be registered for draft invoices
- cross-tenant payment access is blocked
- overpayment is rejected
- invoice read model reflects paid and outstanding amounts correctly

---

## Risks

- weak validation could allow payments on invalid invoice states
- missing tenant scoping would leak financial data across companies
- allowing too much payment mutability too early would weaken auditability

---

## Next Step After This Plan

After this plan, the likely next step is:

- invoice payment status and reporting direction

That phase can introduce:

- `UNPAID`
- `PARTIALLY_PAID`
- `PAID`
- invoice list filters by payment state
- first reporting summaries

---

## Assumptions

- invoice totals are already backend-controlled
- payment provider integrations are out of scope
- first payment phase should stay simple and safe rather than feature-rich

---

## Notes

- This phase should keep payment handling intentionally narrow.
- The main value is trustworthy receivable tracking, not payment workflow complexity.

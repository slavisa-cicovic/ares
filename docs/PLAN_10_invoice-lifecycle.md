# PLAN_10_invoice-lifecycle.md

## Project
Ares

## Goal
Extend the invoice module from draft foundation into the first real invoice lifecycle step.

---

## Summary

The invoice foundation phase introduced:

- tenant-safe invoice aggregate
- draft invoice create/list/detail/update
- backend total calculation
- invoice line snapshots

This phase should move invoices from pure draft data entry into the first real business lifecycle rule:

- a draft invoice can be finalized and issued
- an issued invoice becomes restricted for further modification

This is the phase where invoices start behaving like accounting/business records rather than editable forms.

---

## Scope

This plan covers:

- invoice status transition from `DRAFT` to `ISSUED`
- issue/finalize endpoint
- immutability rules for issued invoices
- read behavior for issued invoices
- integration tests for lifecycle rules

This plan does not cover:

- payment registration
- cancellation or storno flow
- credit notes
- recurring invoices
- PDF generation
- email sending

---

## Why This Comes Next

The backend already has:

- invoice draft foundation
- backend totals
- invoice lines as snapshots
- tenant-safe aggregate access

Without lifecycle rules, invoices are still only editable draft records.

The next natural step is to define:

- when a draft becomes official
- what changes are blocked after issuing
- how clients can rely on invoice state

---

## Core Decisions

This phase should lock the following:

- `DRAFT` invoices can be edited
- `ISSUED` invoices cannot be freely edited like drafts
- issuing is a backend-controlled state transition
- invoice totals and snapshot lines at issue time are the official record
- cross-tenant invoice lifecycle actions remain impossible

---

## Status Direction

Recommended lifecycle status set after this phase:

- `DRAFT`
- `ISSUED`

Required transition in this phase:

- `DRAFT -> ISSUED`

Not included yet:

- `PAID`
- `CANCELLED`
- `VOIDED`
- `STORNO`

Reason:

- those states depend on payments and cancellation policy
- `ISSUED` is the smallest meaningful next step

---

## Endpoints

Existing endpoints remain:

- `GET /invoices`
- `POST /invoices`
- `GET /invoices/{id}`
- `PUT /invoices/{id}`

New endpoint in this phase:

- `POST /invoices/{id}/issue`

Optional future endpoint, but not in this phase:

- `POST /invoices/{id}/cancel`

---

## Issue Behavior

Issuing an invoice should:

- require authenticated tenant company-admin access
- require the invoice to belong to the authenticated company
- require current invoice status to be `DRAFT`
- transition invoice status to `ISSUED`
- preserve current totals and line snapshot values as the official issued record
- return the updated invoice response

One implementation rule should be explicit:

- the backend should not recalculate values from unrelated external state during issuing
- it should use the invoice's current aggregate state and its stored line snapshot data

---

## Immutability Direction

This phase should define what becomes immutable after issuing.

Recommended v1 direction:

Once `ISSUED`, the following are no longer editable:

- client
- issue date
- due date
- currency
- note
- invoice lines
- totals

That means:

- `PUT /invoices/{id}` is allowed only for `DRAFT`
- `PUT /invoices/{id}` on an issued invoice should return a conflict-style business response

Recommended response direction:

- `409 Conflict`
- message such as `Issued invoice cannot be modified.`

This is the cleanest first lifecycle rule.

---

## Authorization Direction

Recommended direction:

- `company-admin` can issue draft invoices
- `company-admin` can edit only draft invoices
- regular `user` can read invoices
- regular `user` cannot issue invoices

This keeps invoice write behavior aligned with earlier business modules while the aggregate grows in complexity.

---

## Tenant Enforcement Rules

Required direction:

- issue action is scoped to authenticated company
- invoice issue lookup must use `id + company_id`
- cross-tenant invoice issue attempts return `404`
- invoice reads remain tenant-scoped
- tenant context continues to come only from backend auth context

---

## Validation Direction

This phase should validate:

- only `DRAFT` invoices may be issued
- only `DRAFT` invoices may be updated
- repeated issue on already issued invoice returns conflict
- issued invoices remain readable

Recommended pragmatic direction:

- do not add extra business checks unless clearly needed now
- the primary new validation is state transition integrity

---

## API Response Direction

This phase should preserve the current invoice response contract.

New behavior:

- issue endpoint returns full invoice response in `ISSUED` state
- update endpoint returns conflict if invoice is already issued

Suggested response codes:

- `200` for successful issue
- `409` for invalid state transition
- `404` for missing or cross-tenant invoice

---

## Persistence Direction

This phase may not require new tables.

Possible persistence changes:

- extend invoice status enum usage to include `ISSUED`
- optional `issued_at` timestamp if we decide it is useful now

My recommendation:

- add `issued_at` now

Reason:

- issuing is a lifecycle event worth timestamping
- it will be useful later for reporting, PDF rendering, and payment behavior

Suggested persistence addition:

- `issued_at timestamp with time zone null`

Rules:

- `issued_at` is `null` while `DRAFT`
- `issued_at` is set when invoice becomes `ISSUED`

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can issue own draft invoice
- regular user cannot issue invoice
- cross-tenant issue returns `404`
- issued invoice is returned with status `ISSUED`
- issuing sets `issued_at` if that field is added to API response or can be verified indirectly
- issued invoice cannot be updated
- draft invoice can still be updated before issue
- repeated issue returns conflict

---

## Deliverables

- invoice issue endpoint exists
- invoice status transition `DRAFT -> ISSUED` exists
- issued invoice immutability rules are enforced
- integration tests cover allowed and denied lifecycle paths

---

## Acceptance Criteria

- company-admin can issue a draft invoice in own company
- issued invoice cannot be edited like a draft
- cross-tenant issue access is blocked
- invoice state behavior is predictable and test-covered
- the invoice module now behaves like a business record, not only a draft editor

---

## Risks

- weak state transition rules would undermine invoice integrity
- allowing post-issue edits too early would create audit and accounting problems
- mixing payment semantics into this phase would blur lifecycle boundaries

---

## Next Step After This Plan

After this plan, the likely next step is:

- `PLAN_11_invoice-payments.md`

That phase should introduce payment recording, open balance tracking, and later paid-status direction.

---

## Assumptions

- invoice foundation remains unchanged except for lifecycle behavior
- issued invoice should be treated as an immutable business record in v1
- payment state is not part of this phase

---

## Notes

- This is the phase where invoices become operationally meaningful.
- The main value is not the endpoint itself, but the rule that issuing freezes the business meaning of the invoice.

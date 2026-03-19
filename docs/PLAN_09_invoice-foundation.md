# PLAN_09_invoice-foundation.md

## Project
Ares

## Goal
Implement the invoice foundation module as the first aggregate business flow.

---

## Summary

This plan introduces the first backend module that combines multiple existing domains:

- company
- client
- item
- authenticated tenant user

The first invoice phase should focus on draft invoices only.

The module should allow:

- listing invoices for the current company
- creating draft invoices
- fetching one invoice
- updating draft invoices
- soft-deleting draft invoices if we decide delete is allowed in v1

This phase is important because invoices are the first aggregate with business calculations, line items, and state management.

---

## Scope

This plan covers:

- `invoice` feature package implementation in `server`
- invoice header and invoice line persistence model
- tenant-safe invoice list/detail/create/update behavior
- draft-only invoice lifecycle in v1
- server-side total calculation
- validation rules for invoice headers and lines
- integration tests for tenant-safe invoice flows

This plan does not cover:

- PDF generation
- email sending
- final issue workflow beyond draft foundation
- payment registration
- cancellation/storno logic
- recurring invoices
- advanced numbering customization

---

## Why This Comes Next

The backend already has:

- auth and token flow
- tenant model
- company and user administration
- `client` tenant business module
- `item` tenant business module

That is the right minimum foundation before invoices.

Invoices should come now because:

- they are the core business value of the product
- they exercise tenant safety across multiple related entities
- they force us to define totals, line snapshots, and state rules early

---

## Core Decisions

This phase should lock the following:

- invoices are always tenant-scoped to one company
- each invoice belongs to exactly one company through `company_id`
- each invoice belongs to exactly one client through `client_id`
- invoice lines belong to one invoice
- invoice totals are calculated on the backend, never trusted from client input
- invoice lines store snapshot values needed for future historical correctness
- invoice lifecycle in v1 is limited to `DRAFT`
- cross-tenant invoice access must be impossible through normal business endpoints

---

## Endpoints

Suggested endpoints for v1:

- `GET /invoices`
- `POST /invoices`
- `GET /invoices/{id}`
- `PUT /invoices/{id}`

Optional for v1:

- `DELETE /invoices/{id}`

If delete is included in v1, my recommendation is:

- allow delete only while invoice is `DRAFT`
- use soft delete if we want future audit consistency

---

## Invoice Aggregate Shape

Suggested invoice header fields:

- `id`
- `company_id`
- `client_id`
- `invoiceNumber`
- `issueDate`
- `dueDate`
- `currency`
- `status`
- `note`
- `subtotal`
- `taxTotal`
- `total`
- `created_at`
- `updated_at`
- `deleted_at`

Suggested invoice line fields:

- `id`
- `invoice_id`
- `item_id` optional reference
- `lineNumber`
- `itemName`
- `itemCode`
- `unit`
- `quantity`
- `unitPrice`
- `taxRate`
- `lineSubtotal`
- `lineTaxTotal`
- `lineTotal`

Important direction:

- line values must be stored as invoice snapshots
- invoice lines should not depend on future item edits
- `item_id` may be kept as an optional trace reference, but invoice rendering must rely on stored snapshot data

---

## Invoice Status Direction

For this first phase, keep the lifecycle intentionally small.

Recommended v1 status:

- `DRAFT`

If we need one extra forward-looking status enum now, it can exist in code, but the plan should only implement `DRAFT` behavior.

Reason:

- invoice lifecycle complexity grows quickly
- totals, validation, and aggregate consistency are the real priorities in this phase

---

## Invoice Number Direction

One decision must be made early: how invoice numbers are assigned.

Recommended v1 direction:

- generate invoice number on the backend
- numbering is unique within one company
- numbering is simple and sequential enough for v1

Pragmatic examples:

- `2026-0001`
- `INV-2026-0001`

What matters most in v1:

- stable backend-owned numbering
- tenant uniqueness
- no reliance on client-generated numbers

If this is still too early to lock fully, we can:

- create the field
- allow draft number generation with a simple internal rule
- postpone richer numbering customization to a later plan

---

## Validation Direction

This phase should validate:

- client belongs to authenticated company
- all referenced items belong to authenticated company when item references are supplied
- at least one invoice line exists
- quantity is positive
- unit price is non-negative
- tax rate is non-negative and within reasonable range
- dates are valid
- due date business rule is sensible for v1

Recommended pragmatic direction:

- keep date validation simple
- focus on aggregate integrity and numeric correctness
- do not over-model jurisdiction-specific invoice rules yet

---

## Total Calculation Direction

This plan should explicitly lock calculation ownership.

Required rule:

- the backend calculates line totals and invoice totals

Suggested calculation direction:

- `lineSubtotal = quantity * unitPrice`
- `lineTaxTotal = lineSubtotal * taxRate / 100`
- `lineTotal = lineSubtotal + lineTaxTotal`
- `subtotal = sum(lineSubtotal)`
- `taxTotal = sum(lineTaxTotal)`
- `total = sum(lineTotal)`

One implementation detail to settle:

- rounding policy

Recommended v1 direction:

- use `BigDecimal`
- round monetary values consistently to scale 2 using one explicit backend rule

This should be documented in implementation, because invoice math bugs are high-cost.

---

## Authorization Direction

This plan should explicitly define who can manage invoices.

Recommended direction for v1:

- `company-admin` can create and update draft invoices
- regular `user` can read invoices

One business decision to settle during implementation:

- should regular `user` also be allowed to create draft invoices

My recommendation:

- keep v1 aligned with `client` and `item`
- `company-admin` write
- `user` read

That keeps permission rules consistent while the aggregate is still being shaped.

---

## Tenant Enforcement Rules

Required direction:

- invoice list is scoped to authenticated company
- invoice detail fetch is scoped to authenticated company
- invoice update is scoped to authenticated company
- invoice delete, if implemented, is scoped to authenticated company
- repository queries must always include tenant scope
- invoice lookup by id must use `id + company_id`, not plain `id`
- client validation must ensure `client.company_id == authenticated company`
- item validation must ensure `item.company_id == authenticated company`
- inaccessible tenant resources should return `404`

Company scope must come from backend auth context, never from client input.

Implementation direction:

- do not use unscoped repository access such as `findById(id)` for tenant business reads or writes
- invoice creation and updates derive `company_id` from authenticated tenant context
- invoice requests must not provide trusted company ownership input

---

## Package Direction

Expected feature structure:

- `features/invoice/controller`
- `features/invoice/dto`
- `features/invoice/entity`
- `features/invoice/repository`
- `features/invoice/service`

This should follow the same module conventions already established in the backend.

---

## API Response Direction

The module should follow existing backend conventions:

- DTO-based contracts
- paged list response shape
- `201` for create
- `200` for read/update
- `404` for inaccessible or missing tenant-scoped invoice

Response DTOs should include:

- invoice header data
- totals
- line item snapshots

The response should be practical for immediate `desktop` and future `mobile` use.

---

## Persistence Direction

This phase likely requires:

- a new `invoices` table
- a new `invoice_lines` table
- mandatory tenant ownership through `company_id`
- foreign key to `clients`
- optional trace foreign key to `items`
- indexes for tenant-scoped lookups and invoice number uniqueness
- timestamps
- optional soft-delete field

Suggested minimum persistence fields for `invoices`:

- `id`
- `company_id`
- `client_id`
- `invoice_number`
- `issue_date`
- `due_date`
- `currency`
- `status`
- `note`
- `subtotal`
- `tax_total`
- `total`
- `deleted_at`
- `created_at`
- `updated_at`

Suggested minimum persistence fields for `invoice_lines`:

- `id`
- `invoice_id`
- `item_id`
- `line_number`
- `item_name`
- `item_code`
- `unit`
- `quantity`
- `unit_price`
- `tax_rate`
- `line_subtotal`
- `line_tax_total`
- `line_total`
- `created_at`

Required persistence rule:

- `company_id` is not optional
- every invoice row belongs to exactly one company
- tenant isolation must exist in the schema model as well as in service/repository logic

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can create draft invoice in own company
- regular user can read invoice list if read access is enabled
- regular user cannot create or update draft invoice if write access is admin-only
- invoice list returns only own-company invoices
- cross-tenant invoice detail returns `404`
- cross-tenant update returns `404`
- create fails if client belongs to another company
- create fails if referenced item belongs to another company
- invalid payload returns `400`
- totals are calculated by backend and returned correctly
- draft update replaces or updates line structure correctly
- duplicate invoice number conflicts if manual override ever exists

---

## Deliverables

- `invoice` feature module exists
- Flyway migrations for invoices and invoice lines exist
- draft invoice create/list/detail/update endpoints exist
- tenant-safe behavior is enforced
- backend total calculation is enforced
- integration tests cover happy and denial paths

---

## Acceptance Criteria

- authenticated tenant users can read invoices according to role rules
- company-admin can create and update draft invoices in own company
- cross-tenant access is blocked
- invoice totals are calculated server-side
- invoice lines store enough snapshot data for historical correctness
- the module provides a stable foundation for later invoice lifecycle work

---

## Risks

- weak tenant scoping would be a critical flaw
- weak rounding rules would create financial inconsistencies
- relying too much on live item data would break historical invoice correctness
- trying to solve full invoice lifecycle now would slow delivery significantly

---

## Next Step After This Plan

After this plan, the likely next step is:

- `PLAN_10_invoice-lifecycle.md`

That phase should extend draft invoices into issue/finalization rules, status transitions, and later payment-related behavior.

---

## Assumptions

- one invoice belongs to one company
- one invoice belongs to one client
- invoice lines are stored as snapshots
- auth and token flow remain unchanged in this phase
- invoice foundation should be draft-first, not full-lifecycle from day one

---

## Notes

- This is the first aggregate where backend business rules matter more than plain CRUD.
- The biggest value of this phase is not only creating draft invoices, but establishing the invariant that invoice math and invoice snapshots belong to the backend.

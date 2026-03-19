# PLAN_08_item-module.md

## Project
Ares

## Goal
Implement the tenant item catalog module.

---

## Summary

This plan introduces the second real tenant business module after `client`.

The module should allow:

- listing items for the current company
- creating items
- fetching one item
- updating item data
- soft-deleting items

This phase is important because it establishes the reusable pattern for catalog data that later invoice flows will depend on.

---

## Scope

This plan covers:

- `item` feature package implementation in `server`
- item CRUD endpoints
- tenant-safe list/detail/update/delete behavior
- validation rules for core item fields
- pagination, sorting, filtering, and search for item lists
- soft delete behavior
- integration tests for tenant-safe item flows

This plan does not cover:

- item categories
- stock tracking
- warehouses
- price history
- discount rules
- invoice line integration

---

## Why This Comes Next

The backend already has:

- auth and token flow
- tenant model
- company and user administration
- `client` tenant business module
- backend conventions in `server/AGENT.md`

The next natural step is the item catalog because:

- invoices will depend on items
- it is still simpler than invoice aggregates
- it reinforces the same tenant-safe CRUD pattern before moving into more complex business flows

---

## Core Decisions

This phase should lock the following:

- items are always tenant-scoped to one company
- each item belongs to exactly one company through `company_id`
- items use soft delete, not physical delete
- normal item queries exclude soft-deleted records
- item APIs are available to authenticated tenant users with role-based access
- cross-tenant item access must be impossible through normal business endpoints

---

## Endpoints

Suggested endpoints:

- `GET /items`
- `POST /items`
- `GET /items/{id}`
- `PUT /items/{id}`
- `DELETE /items/{id}`

These should follow the backend conventions already established for:

- resource naming
- pagination
- sorting
- validation
- soft delete

---

## Item Fields

Suggested initial fields:

- `id`
- `name`
- `code`
- `description`
- `unit`
- `price`
- `taxRate`
- `status`

Recommended status direction:

- `ACTIVE`
- `INACTIVE`

Notes:

- `code` should be optional in the first version unless we decide to force an internal item code
- `price` should represent the current default unit price
- `taxRate` should be stored explicitly because invoices will need stable item tax behavior

---

## Validation Direction

This phase should validate:

- required item name
- field length limits
- non-negative price
- allowed tax rate format/value range
- required unit if we decide unit is mandatory in v1

Rules:

- keep the first version practical
- do not over-design product catalog rules too early
- prefer simple business-safe validation over overly rigid rules

Recommended pragmatic direction:

- validate presence, length, decimal scale, and basic numeric ranges
- leave advanced pricing and tax policy rules for later phases

---

## List Endpoint Behavior

`GET /items` should support:

- pagination
- limited sorting
- optional status filter
- optional `search`

Search direction for v1:

- search by item name
- optionally search by code

Sorting should use explicit whitelisting only.

Suggested default sort:

- `name,asc`

---

## Authorization Direction

This plan should explicitly define who can manage items.

Recommended direction:

- `company-admin` can create, update, and delete items
- regular `user` can read items

My recommendation for v1:

- allow `company-admin` full CRUD
- allow regular `user` read access only

That keeps the first item module aligned with the `client` module and avoids permission drift between early business features.

---

## Tenant Enforcement Rules

Required direction:

- item list is scoped to authenticated company
- item detail fetch is scoped to authenticated company
- item update is scoped to authenticated company
- item delete is scoped to authenticated company
- repository queries must always include tenant scope
- item lookup by id must use `id + company_id`, not plain `id`
- inaccessible tenant resources should return `404`

Company scope must come from backend auth context, never from client input.

Implementation direction:

- do not use unscoped repository access such as `findById(id)` for tenant business reads or writes
- use tenant-scoped query patterns such as:
  - `findByIdAndCompanyIdAndDeletedAtIsNull(...)`
  - `findPageByCompanyIdAndDeletedAtIsNull(...)`
- create operations derive `company_id` from authenticated tenant context
- item requests must not provide trusted company ownership input

---

## Soft Delete Rules

Deleting an item should:

- set a soft-delete marker
- exclude the item from normal list and detail reads
- not physically remove the database row

This keeps later invoice and audit scenarios workable.

---

## Package Direction

Expected feature structure:

- `features/item/controller`
- `features/item/dto`
- `features/item/entity`
- `features/item/repository`
- `features/item/service`

This should mirror the proven module structure from `client`.

---

## API Response Direction

The module should follow existing backend conventions:

- DTO-based contracts
- paged list response shape
- `201` for create
- `200` for read/update
- `204` for delete
- `404` for inaccessible or missing tenant-scoped item

---

## Persistence Direction

This phase likely requires:

- a new `items` table
- mandatory tenant ownership through `company_id`
- appropriate indexes for tenant-scoped lookups
- soft-delete field
- timestamps

Suggested minimum persistence fields:

- `id`
- `company_id`
- `name`
- `code`
- `description`
- `unit`
- `price`
- `tax_rate`
- `status`
- `deleted_at`
- `created_at`
- `updated_at`

Required persistence rule:

- `company_id` is not optional
- every item row belongs to exactly one company
- tenant isolation must exist in the schema model as well as in service/repository logic

One decision to settle during implementation:

- should item `code` be unique per company

My recommendation:

- yes, if code is present, it should be unique within one company
- no need to require `code` globally or cross-tenant

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can create item in own company
- regular user can read item list
- regular user cannot create/update/delete
- item list returns only own-company items
- cross-tenant item detail returns `404`
- cross-tenant update returns `404`
- cross-tenant delete returns `404`
- update works only within current tenant
- soft-deleted item disappears from normal reads
- invalid request payload returns `400`
- invalid sort field returns `400`
- duplicate item code within the same tenant returns the appropriate conflict response if that rule is introduced

---

## Deliverables

- `item` feature module exists
- Flyway migration for items exists
- CRUD endpoints exist
- tenant-safe behavior is enforced
- integration tests cover happy and denial paths

---

## Acceptance Criteria

- authenticated tenant users can read items according to role rules
- company-admin can manage items in own company
- cross-tenant access is blocked
- soft delete works correctly
- list endpoint supports stable pagination and filtering
- the module provides a reusable pattern for invoice-related catalog usage

---

## Risks

- weak tenant scoping would be a critical flaw
- poor decimal handling for price or tax rate would create financial bugs
- over-designing the item model too early can slow delivery
- inconsistent permission rules with `client` would make the API harder to reason about

---

## Next Step After This Plan

After this plan, the likely next step is:

- `PLAN_09_invoice-foundation.md`

That would allow the project to move from simple tenant CRUD modules into the first aggregate that combines clients, items, totals, and business rules.

---

## Assumptions

- one item belongs to one company
- auth and token flow remain unchanged in this phase
- item CRUD should be a standard tenant-scoped backend module

---

## Notes

- This is the first module that starts shaping the future invoice line catalog.
- The main value is a stable, tenant-safe item catalog that later invoice logic can depend on without reworking the basics.

# PLAN_07_client-module.md

## Project
Ares

## Goal
Implement the first real tenant business module: client management.

---

## Summary

This plan introduces the first non-admin tenant business entity after completing auth, token flow, and company/user administration.

The module should allow:

- listing clients for the current company
- creating clients
- fetching one client
- updating client data
- soft-deleting clients

This phase is important because it establishes the repeatable implementation pattern for later business modules such as items, invoices, and payments.

---

## Scope

This plan covers:

- `client` feature package implementation in `server`
- client CRUD endpoints
- tenant-safe list/detail/update/delete behavior
- validation rules for core client fields
- pagination, sorting, and search for client lists
- soft delete behavior
- integration tests for tenant-safe client flows

This plan does not cover:

- client contacts as a separate sub-entity
- client notes/history
- import/export
- invoice integration
- advanced search

---

## Why This Comes Next

The backend already has:

- auth and token flow
- tenant model
- company and user administration
- backend conventions in `server/AGENT.md`

The next natural step is a real tenant business module.
`client` is a good first choice because:

- it is important to the invoicing domain
- it is simpler than invoices
- it exercises the standard CRUD, list, validation, and tenant-scope pattern

---

## Core Decisions

This phase should lock the following:

- clients are always tenant-scoped to one company
- each client belongs to exactly one company through `company_id`
- clients use soft delete, not physical delete
- normal client queries exclude soft-deleted records
- client APIs are available to authenticated tenant users with appropriate role-based access
- cross-tenant client access must be impossible through normal business endpoints

---

## Endpoints

Suggested endpoints:

- `GET /clients`
- `POST /clients`
- `GET /clients/{id}`
- `PUT /clients/{id}`
- `DELETE /clients/{id}`

These should follow the conventions already established for:

- resource naming
- pagination
- sorting
- validation
- soft delete

---

## Client Fields

Suggested initial fields:

- `id`
- `name`
- `pib`
- `mb`
- `email`
- `phone`
- `address`
- `city`
- `postalCode`
- `country`
- `status`

This list can be narrowed if needed, but the first version should cover realistic invoicing use cases.

Recommended status direction:

- `ACTIVE`
- `INACTIVE`

If status feels unnecessary in the first cut, we can rely on soft delete only, but I would keep `status` because it is useful for business workflows.

---

## Validation Direction

This phase should validate:

- required client name
- field length limits
- email format when present
- optional business identifier format where defined

Rules:

- do not over-validate country-specific formats unless we explicitly need that now
- keep the first version practical and maintainable

Recommended pragmatic direction:

- validate presence, length, and basic format
- leave strict domain-country validation for later refinement

---

## List Endpoint Behavior

`GET /clients` should support:

- pagination
- limited sorting
- optional status filter
- optional `search`

Search direction for v1:

- search by client name
- optionally search by email and PIB if useful

Sorting should use explicit whitelisting only.

Suggested default sort:

- `name,asc`

---

## Authorization Direction

This plan should explicitly define who can manage clients.

Recommended direction:

- `company-admin` can create, update, and delete clients
- regular `user` can at least read clients

One policy choice to settle during implementation:

- should regular `user` also be allowed to create and edit clients in v1

My recommendation:

- allow `company-admin` full CRUD
- allow regular `user` read access only in the first cut

That gives us a clean permission boundary for the first business module.

---

## Tenant Enforcement Rules

Required direction:

- client list is scoped to authenticated company
- client detail fetch is scoped to authenticated company
- client update is scoped to authenticated company
- client delete is scoped to authenticated company
- repository queries must always include tenant scope
- client lookup by id must use `id + company_id`, not plain `id`
- inaccessible tenant resources should return `404`

Company scope must come from backend auth context, never from client input.

Implementation direction:

- do not use unscoped repository access such as `findById(id)` for tenant business reads or writes
- use tenant-scoped query patterns such as:
  - `findByIdAndCompanyIdAndDeletedAtIsNull(...)`
  - `findPageByCompanyIdAndDeletedAtIsNull(...)`
- create operations derive `company_id` from authenticated tenant context
- client requests must not provide trusted company ownership input

---

## Soft Delete Rules

Deleting a client should:

- set a soft-delete marker
- exclude the client from normal list and detail reads
- not physically remove the database row

This aligns with the project's backend direction and preserves future audit/history options.

---

## Package Direction

Expected feature structure:

- `features/client/controller`
- `features/client/dto`
- `features/client/entity`
- `features/client/repository`
- `features/client/service`

This should become the reference shape for later domain modules such as `item`.

---

## API Response Direction

The module should follow existing backend conventions:

- DTO-based contracts
- paged list response shape
- `201` for create
- `200` for read/update
- `204` for delete
- `404` for inaccessible or missing tenant-scoped client

---

## Persistence Direction

This phase likely requires:

- a new `clients` table
- mandatory tenant ownership through `company_id`
- appropriate indexes for tenant-scoped lookups
- soft-delete field
- timestamps

Suggested minimum persistence fields:

- `id`
- `company_id`
- `name`
- `pib`
- `mb`
- `email`
- `phone`
- `address`
- `city`
- `postal_code`
- `country`
- `status`
- `deleted_at`
- `created_at`
- `updated_at`

Required persistence rule:

- `company_id` is not optional
- every client row belongs to exactly one company
- tenant isolation must exist in the schema model as well as in service/repository logic

---

## Testing Expectations

This phase should add integration tests for:

- company-admin can create client in own company
- regular user can read client list if read access is enabled
- regular user cannot create/update/delete if those are admin-only
- client list returns only own-company clients
- cross-tenant client detail returns `404`
- cross-tenant update returns `404`
- cross-tenant delete returns `404`
- update works only within current tenant
- soft-deleted client disappears from normal reads
- invalid request payload returns `400`
- invalid sort field returns `400`
- duplicate rules, if any are introduced, return the appropriate conflict response

---

## Deliverables

- `client` feature module exists
- Flyway migration for clients exists
- CRUD endpoints exist
- tenant-safe behavior is enforced
- integration tests cover happy and denial paths

---

## Acceptance Criteria

- authenticated tenant users can read clients according to role rules
- company-admin can manage clients in own company
- cross-tenant access is blocked
- soft delete works correctly
- list endpoint supports stable pagination and filtering
- the module provides a reusable pattern for the next business modules

---

## Risks

- weak tenant scoping would be a critical flaw
- overly strict validation can slow down useful business workflows
- over-designing the client model too early can delay real value
- inconsistent authorization rules now will make later modules harder to align

---

## Next Step After This Plan

After this plan, the likely next step is:

- `PLAN_08_item-module.md`

That would allow the project to establish two clean tenant business modules before moving into invoice complexity.

---

## Assumptions

- one client belongs to one company
- auth and token flow remain unchanged in this phase
- client CRUD should be a standard tenant-scoped backend module

---

## Notes

- This is the first module that should look and feel like actual business product functionality.
- The biggest value is not only client management itself, but the implementation pattern it sets for every later domain module.

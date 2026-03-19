# PLAN_06_company-user-management.md

## Project
Ares

## Goal
Implement the first real administrative business module for tenant management: company profile management and company user management.

---

## Summary

This plan is the first feature-level implementation phase after auth, tenant, and token foundations.

The module should allow:

- reading company profile data for the current tenant
- updating allowed company profile fields
- listing company users
- creating company users
- updating company users
- activating, deactivating, and soft-deleting company users
- assigning allowed roles to company users

This phase should prove that tenant-safe business CRUD can now be built on top of the existing backend foundations.

---

## Scope

This plan covers:

- tenant company profile endpoints
- tenant user management endpoints
- role assignment rules for company users
- tenant-safe list and detail behavior
- admin authorization for company management operations
- integration tests for tenant-safe admin flows

This plan does not cover:

- self-service user profile editing
- password reset
- invitation email flow
- custom company-defined roles management UI or API
- platform-wide company administration screens

---

## Why This Comes Next

The backend already has:

- multi-tenant model
- role model
- token authentication
- current-user contract
- API conventions in `server/AGENT.md`

The next useful, concrete module is company and user administration because it:

- directly exercises auth and tenant rules
- gives `company-admin` a real purpose
- provides foundational admin data needed before feature modules like clients or invoices

---

## Core Decisions

This phase should lock the following:

- `company-admin` can manage users only inside its own company
- tenant-scoped user management is backend-enforced
- `super-admin` behavior must be explicit and separate from normal tenant admin behavior
- users are soft-deleted, not physically deleted
- user lifecycle uses `status` plus `deleted_at`
- role assignment must be limited to allowed built-in tenant roles in this phase

---

## Module Scope

This phase should focus on two related areas:

1. company profile management
2. company user management

These belong together because both are tenant-admin operations and both depend on the same role and tenant rules.

---

## Company Profile Direction

Suggested initial endpoints:

- `GET /company`
- `PUT /company`

Purpose:

- return the current tenant company's profile
- allow controlled update of company profile fields

Suggested initial editable fields:

- company name
- optional business metadata that is safe to expose early

Do not overreach in the first version.
Only fields with clear product value and clear ownership should be editable.

---

## Company User Management Direction

Suggested endpoints:

- `GET /users`
- `POST /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `DELETE /users/{id}`
- optional dedicated activation/deactivation endpoint if lifecycle needs are clearer than full update

Suggested initial capabilities:

- list users of the current company
- fetch one user of the current company
- create a new company user
- update user identity fields and status
- soft-delete a user
- assign built-in tenant roles allowed in this phase

---

## Company User Fields

Suggested initial managed fields:

- email
- first name
- last name
- status
- assigned roles

Rules:

- email remains globally unique in v1
- company is derived from authenticated tenant context for normal tenant-admin flows
- tenant admins must not create users in another company

Password direction for this phase:

- either set an initial password directly for development/admin flow
- or introduce a temporary bootstrap password approach

Recommended pragmatic v1:

- allow admin-set initial password in this phase
- keep the contract explicit
- improve with invitation/reset flow later

---

## Role Assignment Rules

This phase should stay intentionally conservative.

Recommended direction:

- `company-admin` may assign built-in tenant roles only
- allowed built-in tenant roles in this phase:
  - `company-admin`
  - `user`
- `super-admin` remains platform-level and must not be assignable by a normal `company-admin`

Future custom role support remains possible, but should not be coupled into this first management module unless clearly needed now.

---

## Authorization Rules

Required direction:

- `company-admin` can manage users only in its own company
- regular `user` cannot access company administration endpoints
- `super-admin` behavior must be explicitly defined if enabled in these endpoints

My recommendation:

- implement normal tenant-admin behavior first
- optionally allow explicit `super-admin` override only if there is a concrete admin use case in this phase

Do not let platform-level behavior leak accidentally into tenant endpoints.

---

## Tenant Enforcement Rules

This is the critical part of the module.

Requirements:

- all user listing is scoped to authenticated company
- all user detail fetches enforce company ownership
- update and delete operations enforce company ownership
- role assignment cannot cross tenant boundaries

The client must never choose company scope for tenant-admin operations.

---

## Status And Delete Rules

For users:

- use `status` for lifecycle state
- use `deleted_at` for soft delete

Required direction:

- deactivated users remain visible in admin lists where appropriate
- soft-deleted users are excluded from normal reads by default
- deleted users cannot authenticate
- inactive and suspended users cannot authenticate

For company profile:

- this phase is not primarily about deleting companies
- company deletion remains a separate, more sensitive operation

---

## Validation Direction

This module should validate:

- required fields for user creation
- email format
- email uniqueness
- allowed role codes
- allowed status transitions where applicable
- forbidden self-destruction edge cases if we decide to block them

One important policy question for implementation:

- should a `company-admin` be allowed to deactivate or delete themselves

My recommendation:

- block self-delete in v1
- treat self-deactivation carefully and likely block it as well

That avoids easy accidental tenant lockout.

---

## API Direction

The module should follow `server/AGENT.md` conventions:

- resource-oriented endpoints
- DTO-based contracts
- standard list response shape
- soft delete through `DELETE`
- tenant-safe `404` behavior for inaccessible resources

List endpoints should support:

- pagination
- limited sorting
- optional status filter
- optional search by user name or email if useful in v1

---

## Package Direction

Expected feature packages:

- `features/company`
  - `controller`
  - `dto`
  - `entity`
  - `repository`
  - `service`
- `features/user`
  - `controller`
  - `dto`
  - `entity`
  - `repository`
  - `service`

If needed, shared admin authorization helpers may live in `common`, but business logic should stay in feature services.

---

## Testing Expectations

This phase should add integration tests for:

- `company-admin` can read and update own company profile
- regular user is rejected from company admin routes
- `company-admin` can list users only from own company
- `company-admin` can create user in own company
- `company-admin` cannot create user in another company
- `company-admin` can update own-company user
- `company-admin` cannot update another company's user
- `company-admin` can soft-delete own-company user
- soft-deleted user disappears from normal reads
- role assignment rejects forbidden role codes such as `super-admin`
- duplicate email creation is rejected

If explicit `super-admin` override is implemented, that must also be tested separately and explicitly.

---

## Deliverables

- tenant company profile endpoints exist
- tenant user management endpoints exist
- company-admin authorization is enforced
- role assignment rules are enforced
- tenant-safe list/detail/update/delete behavior exists
- integration tests cover happy and denial paths

---

## Acceptance Criteria

- `company-admin` can manage company profile and users for its own tenant
- regular users cannot access tenant admin operations
- cross-tenant user access is blocked
- soft delete and lifecycle rules behave correctly
- user creation and update follow backend validation rules
- the module establishes a repeatable pattern for later business modules

---

## Risks

- accidental cross-tenant access would be a critical flaw
- allowing tenant admins to assign platform roles would break security boundaries
- weak lifecycle rules can create lockout or stale-auth issues
- unclear self-management rules can create admin dead-end scenarios

---

## Next Step After This Plan

After this plan, likely next steps are:

- `PLAN_08_client-module.md`
- or a dedicated plan for custom role management if that becomes urgent

My recommendation:

- go to the first tenant business module after this, likely `client`

---

## Assumptions

- one user belongs to one company in v1
- `company-admin` and `user` are the built-in tenant roles available in this phase
- `super-admin` remains platform-level
- invitation and password reset flows are deferred

---

## Notes

- This is the first module that should feel like real product administration, not just infrastructure.
- The value of this phase is not only the feature itself, but also the repeatable implementation pattern it creates for future modules.

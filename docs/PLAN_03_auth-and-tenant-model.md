# PLAN_03_auth-and-tenant-model.md

## Project
Ares

## Goal
Define the authentication, tenant, and authorization model for Ares so backend implementation can proceed with a clear user identity model, strict company isolation, and an RBAC direction that supports both built-in and future custom roles.

---

## Summary

This plan establishes how users are authenticated, how tenant context is resolved, and how authorization should work in a multi-tenant Ares backend.

This is a foundational plan.
It exists to prevent later rework in:

- database design
- security configuration
- repository patterns
- service boundaries
- API authorization
- user and company feature implementation

The design should be strict enough to protect tenant boundaries from the start, while still leaving room for future RBAC growth.

---

## Scope

This plan covers:

- user identity model
- company tenant model
- authentication direction
- session/token direction at architectural level
- tenant context resolution
- authorization model
- built-in roles
- future custom company roles
- permission catalog direction
- initial auth and tenant schema direction

This plan does not cover:

- final login UI
- password reset flows in detail
- email delivery infrastructure
- SSO or external identity providers
- full audit logging implementation
- every future permission code

---

## Core Decisions

The following decisions should be treated as the current foundation:

- Ares is multi-tenant
- company is the tenant root
- a user belongs to exactly one company in version 1
- tenant identity must come from authenticated backend context
- business data is always scoped to the authenticated user's company unless explicitly platform-global
- authorization must check both tenant scope and role/permission
- the system must support both built-in roles and future company-defined custom roles

---

## Identity Model

### 1. User

For version 1:

- each user has one account
- each user belongs to exactly one company
- each user authenticates as an individual identity
- user status should support at least active vs inactive behavior

User identity should not be treated as a generic company member record without credentials.
The user is the authenticated principal.

### 2. Company

Company is the tenant root.

Rules:

- company owns company-scoped business data
- company-scoped users operate only within that company
- company-scoped custom roles, if introduced later, belong only to one company

### 3. Platform Identity vs Tenant Identity

There are two conceptual scopes:

- platform scope
- company scope

Most users operate in company scope.
`super-admin` operates at platform scope and is not constrained as a regular tenant user in the same way as company users.

---

## Authentication Direction

The authentication system should be designed for API clients: desktop and mobile.

Recommended direction:

- use stateless token-based authentication
- plan around access tokens and refresh tokens
- keep the backend as the only authority for issued identity and claims

This plan does not need to fully implement the token flow yet, but it should lock the architectural direction.

Recommended login flow direction:

1. user submits credentials
2. backend validates credentials
3. backend resolves user status and company membership
4. backend issues authenticated session/token material
5. subsequent API requests carry backend-issued identity context

Initial credential direction:

- email as the primary login identifier
- password stored only as a secure hash

Rules:

- never store plain-text passwords
- never trust client-provided role or tenant claims without backend verification
- inactive users must not authenticate successfully

---

## Tenant Context Resolution

Tenant context must be resolved inside the backend.

Required behavior:

- authenticated identity resolves the user
- resolved user determines the company tenant
- service and repository operations execute within that company scope

Tenant context must not be taken from arbitrary request payload fields as the authority source.

Acceptable use of client-provided company identifiers:

- only for validation or route consistency checks when the backend already knows the authenticated tenant
- never as the sole source of tenant truth

Recommended foundation objects:

- authenticated principal model
- tenant context object
- tenant-aware authorization helper

Outcome:

- tenant scope should be easy to access in service code
- bypassing tenant constraints should require an explicit, reviewable exception path

---

## Authorization Model

Authorization should be RBAC-based, with a permission-oriented direction for future expansion.

The system should support:

- platform-level roles
- tenant-level built-in roles
- tenant-level custom roles in later phases
- backend-defined permission catalog

Authorization rule:

- access is granted only if both the tenant scope and required permission conditions are satisfied

This prevents a valid tenant user from accessing data outside their company even if they hold a high role inside their own company.

---

## Built-In Roles

### 1. `super-admin`

This is a platform-level role.

Intended purpose:

- platform administration
- company lifecycle operations
- platform support and maintenance actions
- cross-tenant administration when explicitly allowed by the backend

Rules:

- `super-admin` is not a company-defined role
- companies must not be able to edit or assign platform-global privileges outside approved backend rules

### 2. `company-admin`

This is a tenant-level built-in role.

Intended purpose:

- manage company users
- manage company settings
- manage company-level access configuration
- perform broad business administration within one company

Rules:

- authority is limited to the user's own company
- the role must not escape tenant boundaries

### 3. `user`

This is the baseline tenant-level role.

Intended purpose:

- standard business usage within the user's company
- access only to explicitly assigned or default-permitted operations

Rules:

- `user` should not imply administrative rights by default

---

## Future Custom Company Roles

The architecture should support company-defined roles later.

Direction:

- a `company-admin` can define custom roles inside their own company
- custom roles are composed from backend-defined permissions
- custom roles are scoped to a single company only
- custom roles cannot create new permission identifiers

This means:

- permission definitions are controlled centrally in the backend
- companies choose from allowed permissions when composing custom roles
- cross-tenant privilege escalation is structurally blocked

Examples of future custom roles:

- `manager`
- `accountant`
- `sales`

These names are company-local role labels, not platform-global authority categories.

---

## Permission Catalog Direction

Permissions should be backend-defined, explicit, and stable.

Recommended structure:

- module-oriented permission codes
- action-oriented naming

Example direction:

- `client.read`
- `client.write`
- `invoice.read`
- `invoice.write`
- `payment.write`
- `user.manage`
- `role.manage`

Rules:

- permission codes are defined by the platform
- built-in roles map to known permission sets
- custom company roles reuse the same permission catalog

This keeps authorization consistent across desktop, mobile, and backend enforcement.

---

## Data Model Direction

The database should support both the current simple rules and future RBAC expansion.

Initial required concepts:

- `companies`
- `users`
- built-in role assignment
- user status
- password hash
- soft delete support
- audit timestamps

Future-ready concepts that should influence the model now:

- company custom roles
- role-permission mapping
- user-role assignment beyond one hard-coded role column if needed

Pragmatic recommendation:

- do not over-normalize every future RBAC capability in the first migration
- but do avoid a schema that traps the system into one fixed role forever

Good direction:

- start with tables that can expand into role assignment and permission mapping
- keep built-in roles explicit
- treat custom roles as a planned extension, not an afterthought
- do not rely on physical deletes for core auth and tenant records

---

## Initial Schema Direction

Later migrations for auth and tenant foundation should prepare at least:

- `companies`
- `users`
- `roles`
- `permissions`
- `role_permissions`
- `user_roles`

Recommended interpretation:

- roles should distinguish platform-level vs company-level scope
- built-in roles should be seedable by migration or bootstrap logic
- custom company roles should reference the owning company when introduced
- platform roles should remain globally controlled
- `users` and `companies` should use soft delete, not physical delete
- lifecycle state and deletion state should be modeled separately

Soft delete direction:

- use `status` for lifecycle state such as active, inactive, or suspended
- use `deleted_at` as the deletion marker
- active application queries should exclude soft-deleted records by default
- restore should remain possible if future business rules require it

User-related fields should likely include:

- `id`
- `company_id`
- `email`
- `password_hash`
- `first_name`
- `last_name`
- `status`
- `deleted_at`
- `created_at`
- `updated_at`

Company-related fields should likely include:

- `id`
- `name`
- `status`
- `deleted_at`
- `created_at`
- `updated_at`

The exact physical schema can be finalized during implementation, but this plan should drive it.

---

## Security Enforcement Direction

Authorization must be enforced in backend code paths, not merely represented in DTOs or UI behavior.

Required enforcement layers:

- authentication filter or auth entry layer
- security configuration
- method or service authorization checks where needed
- repository patterns that respect tenant scope

Rules:

- controllers must not be the only security boundary
- repositories must not expose easy unrestricted cross-tenant reads
- `super-admin` exception paths must be explicit and rare

---

## API Direction

The auth model will affect API design.

Expected API categories:

- auth endpoints
- current user/session endpoints
- company administration endpoints
- company user management endpoints
- future role and permission management endpoints

Recommended direction:

- keep auth endpoints under a dedicated auth feature
- expose an authenticated "who am I" style endpoint early
- use backend responses, not client assumptions, to determine the current user's role and tenant

---

## Deliverables

- a documented v1 user-to-company model
- a documented distinction between platform roles and tenant roles
- a documented RBAC direction with future custom company roles
- a documented tenant context resolution rule
- a documented initial schema direction for auth and tenant data
- a clear foundation for implementation in `server`

---

## Acceptance Criteria

- the plan clearly states that one user belongs to exactly one company in v1
- the plan clearly distinguishes `super-admin`, `company-admin`, and `user`
- the plan clearly states that tenant identity comes from authenticated backend context
- the plan clearly states that tenant isolation must be enforced in backend authorization and persistence paths
- the plan leaves room for future custom company roles without requiring a complete redesign

---

## Risks

- putting tenant identity in client-controlled input can create cross-tenant data leaks
- modeling roles too narrowly can block future company-defined roles
- modeling RBAC too abstractly too early can slow implementation
- mixing platform roles and tenant roles carelessly can create privilege escalation bugs

---

## Next Step After This Plan

After this plan, backend implementation can proceed more safely with:

- `PLAN_02_server-foundation.md` execution
- initial Flyway migrations for company/user/auth foundation
- Spring Security baseline implementation
- auth feature package structure

Likely next planning documents after implementation starts:

- `PLAN_04_desktop-foundation.md`
- `PLAN_05_mobile-foundation.md`
- later feature plans for clients, items, invoices, and payments

---

## Assumptions

- company remains the tenant root
- v1 keeps one user in one company
- Ares requires both platform administration and tenant administration
- desktop and mobile will both use backend-issued authentication
- backend remains the source of truth for identity, tenant, and permission decisions

---

## Notes

- Keep auth and tenant rules explicit.
- Prefer a slightly more structured model now over a security-driven rewrite later.

# PLAN_01_ares-foundation.md

## Project
Ares

## Goal
Define the initial foundation for the Ares workspace so implementation can start from a clear backend-first, multi-tenant architecture shared by the desktop app, mobile app, and Spring Boot server.

---

## Summary

This plan establishes the first implementation phase for Ares.
The goal of this phase is not to build business features yet, but to lock the architectural direction, define the tenant model, and prepare the repository structure for incremental delivery.

Ares is a new product.
The old `minifakt` project is reference material only and should be used to inform design decisions, not to define the new source layout or persistence model.

---

## Scope

This plan covers:

- workspace-level architecture direction
- multi-tenant model
- backend responsibilities
- desktop responsibilities
- mobile responsibilities
- initial domain scope
- initial documentation structure
- implementation order for next phases

This plan does not cover:

- full API endpoint definitions
- final authentication implementation
- final database schema
- detailed UI design
- offline mobile strategy
- deployment and infrastructure details

---

## Target Architecture

The system should evolve toward one shared business backend with multiple clients:

- `server` is the source of truth for business data and business rules
- `desktop` is a client application for business users
- `mobile` is a React Native client application for the same backend
- `docs` contains architectural decisions and implementation plans

Core interaction model:

- clients authenticate against the backend
- clients call explicit backend APIs
- backend resolves tenant context from authenticated user identity
- backend persists and reads data within tenant boundaries only

The old local-desktop pattern from `minifakt` must not be carried forward as the core architecture.
Desktop is no longer the place where persistence and business rules should live.

---

## Multi-Tenant Model

Tenant isolation is a first-order requirement.

Rules:

- every user belongs to one company tenant
- every business record belongs to one company tenant unless explicitly global
- data from one company must never be visible to another company
- tenant filtering must be enforced in the backend, not only in clients
- queries, writes, validation, and authorization must all respect tenant boundaries

Initial tenant-oriented domain assumption:

- `company` is the tenant root
- `user` belongs to one `company`
- business modules such as clients, items, invoices, payments, and reports are company-scoped

Open point for later plan:

- whether some users may eventually belong to multiple companies should be treated as out of scope unless explicitly planned

---

## Workspace Responsibilities

### 1. `server`

The Spring Boot backend is responsible for:

- authentication and authorization
- tenant resolution
- business rules
- validation at API boundaries
- persistence
- migrations
- reporting logic

Implementation direction:

- organize code by feature/domain
- keep controllers thin
- use services for business use cases
- use repositories for persistence access
- use Flyway for schema evolution

### 2. `desktop`

The desktop application is responsible for:

- authenticated business UI
- navigation and workflows
- forms, tables, and local interaction state
- calling backend APIs through explicit client modules

Desktop constraints:

- desktop is a new codebase
- old `minifakt` may guide UX and module boundaries, but not dictate implementation
- persistence logic must not become embedded in Electron layers
- Electron-specific code should stay minimal and isolated

### 3. `mobile`

The mobile application is responsible for:

- mobile-oriented user workflows
- authenticated access to the same backend domain
- UI and local interaction state suited for React Native

Mobile constraints:

- use React Native
- keep business logic thin in the client
- align models and flows with backend contracts

### 4. `docs`

The documentation folder is responsible for:

- master plans
- phase plans
- architectural notes
- migration notes
- major implementation decisions

---

## Initial Domain Scope

The first business scope should be limited to the operational core:

- users
- companies
- company settings
- clients
- items/services
- invoices
- payments
- reports

Recommended interpretation:

- users and companies are foundation modules
- settings, clients, items, invoices, and payments are the main MVP business modules
- reports should initially stay basic and derive from transactional data

Out of scope for early phases:

- complex accounting/ERP behavior
- payroll
- advanced inventory accounting
- broad integrations with third-party or government systems

---

## Backend Foundation Direction

The first backend implementation phases should establish:

- package structure by feature
- application configuration structure
- database migration strategy with Flyway
- persistence model with explicit tenant ownership
- authentication/security foundation
- shared API error handling approach

The backend should be designed so tenant enforcement is structurally difficult to bypass.

Preferred direction:

- authenticated request establishes user identity
- backend derives company context from that identity
- business services operate within resolved company scope
- repositories never expose unrestricted cross-tenant access by default

---

## Client Integration Direction

Both clients should integrate against backend contracts, not against database assumptions.

Recommended principles:

- define client-side API modules explicitly
- avoid mixing view logic and transport logic
- keep client-side domain models aligned with server DTOs and workflows
- centralize auth/session handling per client

Desktop-specific note:

- if Electron preload is used, it should remain a narrow transport/security boundary, not a replacement backend

---

## Migration From Old MiniFakt

The old `minifakt` project should be used selectively.

What may be reused conceptually:

- screen structure
- functional modules
- workflow ideas
- table/form composition patterns
- invoice and payment concepts

What should not be copied without explicit review:

- SQLite-centered architecture
- Electron main process as business backend
- IPC contracts shaped around local persistence
- repository/service boundaries designed for local desktop storage

Migration principle:

- port concepts deliberately
- re-implement in the new architecture

---

## Deliverables For This Phase

- root `AGENT.md` exists and defines workspace rules
- `PLAN_01` exists and defines the initial architectural direction
- repository metadata files exist
- Spring Boot server skeleton is present
- next plans can be created without reopening core architecture questions

---

## Acceptance Criteria

- the repository clearly documents that Ares is a new product, not a direct continuation of `minifakt`
- the repository clearly documents that Ares is multi-tenant
- desktop, mobile, and server responsibilities are clearly separated
- backend-first architecture is explicit
- next implementation plans can reference this document as the foundation

---

## Next Plans

After this document, the next recommended plans are:

1. `PLAN_02_server-foundation.md`
2. `PLAN_03_auth-and-tenant-model.md`
3. `PLAN_04_desktop-foundation.md`
4. `PLAN_05_mobile-foundation.md`
5. `PLAN_06_core-domain-modules.md`

Suggested order:

- define backend foundation first
- define authentication and tenant enforcement early
- start desktop foundation after backend direction is stable
- start mobile after backend contracts and auth approach are clear

---

## Assumptions

- Spring Boot remains the backend framework
- mobile remains React Native
- desktop remains a separate desktop client
- company is the initial tenant boundary
- implementation should proceed incrementally through plans in `docs`

---

## Notes

- Keep comments and code in English.
- Prefer explicit contracts over hidden conventions.
- Prefer clear tenant-safe design over short-term convenience.

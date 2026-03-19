# PLAN_02_server-foundation.md

## Project
Ares

## Goal
Establish the initial Spring Boot backend foundation in `server` so future feature work can be built on a clear, secure, multi-tenant, backend-first base.

---

## Summary

This plan turns the current Spring Boot skeleton into a usable backend foundation.
The purpose of this phase is to prepare the server for incremental feature delivery, not to implement the full business domain yet.

The result of this phase should be:

- a clear package structure
- environment-aware application configuration
- database connectivity and Flyway migrations
- a minimal security foundation
- a tenant-safe architectural direction
- shared error handling and API conventions

This phase should make it easy to start `users`, `companies`, `auth`, `clients`, `items`, `invoices`, and `payments` in later plans without reworking the server base.

---

## Scope

This plan covers:

- server package structure
- application configuration layout
- PostgreSQL integration
- Flyway setup
- startup conventions
- security foundation
- API error handling foundation
- health/readiness baseline
- test foundation for backend bootstrap

This plan does not cover:

- full authentication flows
- concrete business modules
- all tenant-aware entities
- final production deployment
- advanced observability stack
- background jobs

---

## Current Starting Point

Current state in `server`:

- Spring Boot project exists
- dependencies include Web MVC, Validation, Security, JPA, Flyway, and PostgreSQL
- `application.yaml` is minimal
- no business package structure is present yet
- no Flyway migrations exist yet
- no security configuration is defined yet

This plan assumes we will evolve from this generated skeleton rather than regenerate the project.

---

## Foundation Principles

The backend foundation must support the Ares product rules from `AGENT.md`:

- backend-first architecture
- multi-tenant isolation
- company-scoped business data
- explicit API contracts
- feature-oriented organization
- security enforced in the backend

The key objective is to make the correct architecture the default path.

---

## Implementation

### 1. Package Structure

Create a package layout under `server/src/main/java` that is feature-oriented and still gives room for shared infrastructure.

Recommended top-level structure:

- `com.sc.ares.common`
- `com.sc.ares.config`
- `com.sc.ares.features.auth`
- `com.sc.ares.features.user`
- `com.sc.ares.features.company`
- `com.sc.ares.features.client`
- `com.sc.ares.features.item`
- `com.sc.ares.features.invoice`
- `com.sc.ares.features.payment`
- `com.sc.ares.features.report`

Recommended purpose:

- `common`
  shared error model, shared DTO infrastructure, tenant context utilities, base persistence helpers
- `config`
  Spring configuration classes only
- `features/*`
  feature modules organized by business domain

Preferred feature structure:

- `features/client/controller`
- `features/client/dto`
- `features/client/entity`
- `features/client/repository`
- `features/client/service`

The same pattern should be reused for other business modules where it fits.
This gives each feature a clear local boundary while still keeping the project organized by business capability.

Rule:

- business modules such as `client`, `item`, `invoice`, `payment`, and `report` should be added under `features`
- avoid generic application-wide controller/service/repository mega-folders
- shared cross-feature infrastructure should stay in `common` or `config`, not inside arbitrary feature modules

### 2. Application Configuration

Expand `src/main/resources/application.yaml` into a real configuration baseline.

Configuration should cover:

- application name
- active profile strategy
- datasource settings through environment variables
- Flyway settings
- JPA settings appropriate for explicit schema control
- server port and API path conventions
- logging baseline

Recommended profile direction:

- `application.yaml` for shared defaults
- `application-dev.yaml` for local development
- `application-test.yaml` for tests if needed

Rules:

- keep secrets out of source control
- prefer environment variables for DB credentials and sensitive values
- do not rely on JPA auto-DDL as the schema management strategy

### 3. PostgreSQL Integration

Use PostgreSQL as the server persistence foundation.

Changes:

- wire datasource configuration for PostgreSQL
- confirm app startup behavior when DB is reachable
- fail clearly when DB configuration is invalid

Direction:

- PostgreSQL is the primary transactional database
- schema evolution should go through Flyway
- JPA should not silently create or mutate schema in place

### 4. Flyway Foundation

Set up Flyway as the only approved schema change mechanism.

Implement:

- migration folder structure
- initial migration naming convention
- startup migration execution
- migration strategy documentation in code comments or docs where needed

Recommended naming:

- `V1__init_extensions.sql`
- `V2__init_company_and_user_foundation.sql`

Rules:

- all schema changes must be explicit migrations
- avoid mixing ad hoc schema changes with automatic ORM generation
- migration history must be stable and source-controlled

### 5. Initial Database Foundation

Do not model the entire product yet.
Create only the minimum schema needed to support later auth and tenant plans.

Initial schema direction should prepare for:

- companies
- users
- user-to-company relationship according to the current rule that one user belongs to one company
- basic audit timestamps

The first schema should establish tenant root tables before feature tables such as clients, items, and invoices.

### 6. Security Foundation

Introduce a minimal, explicit Spring Security baseline.

This phase should define:

- one security configuration entry point
- clear public vs protected route policy
- a place for future authentication mechanisms
- a consistent unauthorized/forbidden response shape

Recommended direction for the foundation phase:

- keep only health or bootstrap endpoints public if needed
- protect future business APIs by default
- structure the code so auth implementation can be added without reshaping all controllers

This phase does not need to finalize JWT, session, refresh tokens, or mobile auth details yet.
It must only establish a secure default.

Role and authorization direction for later plans:

- support platform-level roles and company-level roles
- reserve `super-admin` as a platform-level role
- treat `company-admin` and `user` as initial tenant-level built-in roles
- prepare the model so company-defined custom roles can be added later
- keep the permission catalog backend-defined, not tenant-defined
- ensure that custom company roles are scoped to one company only

Implication for the foundation phase:

- do not hard-code the server into a role model that only supports one fixed tenant role
- leave room in package structure and security design for later RBAC expansion

### 7. Tenant Context Direction

The full tenant model will be detailed later, but the server foundation must already prepare for it.

Establish these rules:

- tenant identity is derived from authenticated backend context
- business operations must execute within resolved company scope
- repositories should not expose unrestricted cross-tenant access paths
- service methods should be designed with tenant-safe assumptions from the start

Foundation outcome:

- code structure should have a clear place for tenant context objects, tenant-aware repository patterns, and authorization checks

### 8. API Conventions

Define basic API conventions before feature controllers appear.

Conventions should include:

- base API path, for example `/api`
- JSON request and response defaults
- validation error format
- application error format
- not-found and conflict response handling

Recommendation:

- implement one shared exception handling mechanism
- avoid per-controller ad hoc error shaping

### 9. Health and Bootstrap Endpoints

Provide a minimal operational baseline.

This can include:

- simple health endpoint
- optional version or info endpoint if useful

Rules:

- operational endpoints must not leak sensitive configuration
- keep bootstrap endpoints minimal

### 10. Test Foundation

Prepare a lightweight but useful test baseline.

This phase should include:

- application context startup test
- migration-aware test setup
- a strategy for repository/service tests in later phases

Recommended direction:

- keep tests aligned with real configuration where practical
- ensure migration failures are caught early

---

## Deliverables

- `server` has a clear package structure for future feature work
- PostgreSQL configuration is defined
- Flyway is configured and ready
- initial migration files exist
- security foundation is present
- API error handling foundation is present
- a minimal health/readiness baseline exists
- server startup is intentional rather than generated-default behavior

---

## Acceptance Criteria

- the backend starts with explicit configuration rather than generated defaults only
- PostgreSQL is the intended and configured persistence target
- Flyway manages schema evolution
- JPA auto schema generation is not the primary schema strategy
- security defaults protect future business APIs
- tenant-safe architecture is reflected in the package and code direction
- the project is ready for `PLAN_03_auth-and-tenant-model.md`

---

## Suggested File Direction

Likely files and areas involved:

- `server/src/main/resources/application.yaml`
- `server/src/main/resources/application-dev.yaml`
- `server/src/main/resources/application-test.yaml`
- `server/src/main/resources/db/migration/*`
- `server/src/main/java/com/sc/ares/config/*`
- `server/src/main/java/com/sc/ares/common/*`
- `server/src/main/java/com/sc/ares/features/auth/*`
- `server/src/main/java/com/sc/ares/features/company/*`
- `server/src/main/java/com/sc/ares/features/user/*`
- `server/src/main/java/com/sc/ares/features/client/*`

Exact filenames can be decided during implementation, but the structure should stay feature-oriented and tenant-safe.

---

## Risks

- starting feature work before security and tenant patterns are defined can cause broad rework
- allowing unrestricted repository access early can create hidden cross-tenant bugs
- mixing Flyway and implicit JPA schema generation can produce unstable environments
- over-designing auth before the foundation is stable can slow down the project unnecessarily
- hard-coding authorization too narrowly can block later custom company role support

---

## Next Plans

The next recommended document after this one is:

- `PLAN_03_auth-and-tenant-model.md`

After that, recommended implementation planning can continue with:

- `PLAN_04_desktop-foundation.md`
- `PLAN_05_mobile-foundation.md`
- `PLAN_06_core-domain-modules.md`

---

## Assumptions

- Spring Boot remains the server framework
- PostgreSQL remains the primary database
- Flyway remains the schema migration tool
- company remains the initial tenant boundary
- security will be added incrementally, but the default posture must stay protected

---

## Notes

- Keep code and comments in English.
- Prefer explicit backend contracts over convenience magic.
- Favor tenant-safe defaults even if they require slightly more structure up front.

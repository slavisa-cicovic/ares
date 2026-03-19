# AGENT.md

## Scope
This document defines backend-specific rules for the `server` module.
It extends the root `AGENT.md`.
If there is a conflict, the root `AGENT.md` defines project-wide direction and this file defines backend implementation rules.

## Backend Role
- `server` is the main business backend for Ares
- persistent business data belongs to the backend
- business rules belong primarily in the backend
- desktop and mobile are API clients, not sources of truth

## Multi-Tenant Rules
- tenant scope must always be enforced in backend code
- tenant identity must come from authenticated backend context
- tenant identity must never be trusted from arbitrary client input
- data from different companies must never mix
- repository, service, and controller behavior must preserve tenant isolation
- if a tenant-scoped resource is outside the caller's allowed scope, prefer `404` over leaking cross-tenant existence

## Package Structure
- organize code by feature under `features`
- prefer feature modules such as `features/client`, `features/item`, `features/invoice`
- inside each feature, prefer explicit subpackages:
  - `controller`
  - `dto`
  - `entity`
  - `repository`
  - `service`
- use `common` only for genuinely shared infrastructure
- do not move feature business logic into `common`

## Layer Rules
- controllers stay thin
- request validation starts at the API boundary
- business rules belong in services
- persistence access belongs in repositories
- entities are persistence models, not API contracts
- request DTOs and response DTOs must stay separate from entities

## API Style
- use resource-oriented endpoints
- use plural resource names where appropriate
- keep paths noun-based, not verb-based, for normal CRUD
- acceptable examples:
  - `GET /clients`
  - `POST /clients`
  - `GET /clients/{id}`
  - `PUT /clients/{id}`
  - `DELETE /clients/{id}`
- avoid endpoint styles such as:
  - `GET /getClients`
  - `POST /createClient`
  - `POST /clients/delete`
- action-style endpoints are allowed only for operations that are not natural CRUD

## Resource Identity
- persisted resource IDs are backend-generated UUID values
- path parameters should use UUID resource identifiers
- clients must not generate authoritative persisted IDs for business entities

## List Responses
- list endpoints should use one shared response shape
- preferred fields are:
  - `items`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
- use zero-based page indexing
- define and enforce a central default page size
- define and enforce a central maximum page size

## Sorting And Filtering
- use `sort=field,direction`
- allow only explicitly whitelisted sortable fields
- reject invalid sort fields as validation errors
- do not allow arbitrary property-path sorting from client input
- use query parameters for simple filters
- keep filter semantics explicit per endpoint
- free-text search should stay intentionally limited
- filtering must never allow tenant escape

## Create And Update
- use `POST /resource` for create
- use `PUT /resource/{id}` for full update in early phases
- `PATCH` is optional and should be introduced only when it adds clear value
- create should return the created representation
- update should return the updated representation

## Delete Rules
- no physical delete for core business entities unless explicitly justified
- prefer soft delete for core business entities
- `DELETE /resource/{id}` should normally perform soft delete
- normal list and detail reads must exclude soft-deleted records
- restore behavior, if introduced later, must be explicit

## Validation And Errors
- validate required fields and basic format in DTOs
- enforce business-rule validation in services
- keep API error handling consistent across modules
- baseline status code expectations:
  - `400` invalid request input
  - `401` missing or invalid authentication
  - `403` forbidden operation
  - `404` resource not found within allowed scope
  - `409` clear business conflict when appropriate

## Security Rules
- security must be explicit, never implicit
- authenticated context is the source of truth for user and tenant identity
- role and permission checks must happen in backend code
- any platform-level cross-tenant behavior must be explicit and deliberate
- normal tenant APIs must not silently operate across companies

## Persistence Rules
- use Flyway for schema migrations
- do not rely on JPA schema generation for real environment evolution
- persistence model changes should be reflected in explicit migrations
- queries and write paths must enforce tenant scope where applicable

## Date And Time
- persist timestamps in UTC
- expose timestamps as ISO-8601 strings
- keep date-only values date-only where semantically appropriate
- do not expose database-specific date formats in API contracts

## Lombok Rules
- do not use Lombok in JPA entities
- do not use Lombok in service classes
- do not use Lombok in configuration classes
- Lombok is allowed in DTO classes when it clearly improves readability

## Testing Expectations
- business modules should include integration coverage for:
  - tenant-scoped list and detail behavior
  - invalid sort or filter rejection
  - soft-deleted record exclusion from normal reads
  - authorization and role-sensitive behavior where applicable

## Design Preference
- prefer boring, explicit, maintainable solutions
- avoid premature abstraction
- keep backend contracts intentional and stable

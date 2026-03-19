# AGENT.md

## Project
Ares

## Purpose
Ares is a business application platform centered around:
- desktop application
- mobile application
- Spring Boot backend
- shared business domain for invoicing and small-business workflows

The old `minifakt` project is reference material only.
It may be used as inspiration for UX, flow, data model ideas, and migration strategy.
Its source code is not the source of truth for this repository.
All implementation in `ares` should be treated as new source.

## Workspace Structure
- `docs`
  Planning documents, rollout phases, architectural notes, migration notes
- `desktop`
  New desktop application source
- `mobile`
  New React Native mobile application source
- `server`
  Spring Boot backend source

## Product Direction
The platform should evolve toward one backend-centered architecture:
- `server` is the main business backend
- persistent business data belongs to the backend
- client apps are consumers of backend APIs
- business rules should not be duplicated across clients

Desktop and mobile are separate clients for the same domain.

## Multi-Tenant Model
- Ares is a multi-tenant application
- a user belongs to one company tenant
- business data must always be scoped to the user's company
- data from different companies must never mix
- tenant isolation is a core backend responsibility
- API design, persistence, queries, and authorization must enforce tenant boundaries explicitly
- no client may rely on UI filtering alone for tenant separation

## Architecture Principles
- backend-first architecture
- API contracts must be explicit
- business logic belongs primarily in `server`
- desktop and mobile should stay thin where possible
- UI-specific state belongs in clients
- persistence logic must not live in UI layers
- avoid tight coupling between desktop and mobile implementations

## Server Rules
- use Spring Boot as the application foundation
- organize code by feature/domain, not by technical layer only
- prefer server feature modules under a `features` root, for example `features/client`, `features/item`, `features/invoice`
- inside each feature, prefer explicit subpackages such as `controller`, `dto`, `entity`, `repository`, and `service`
- use Flyway for schema migrations
- validate external input at API boundaries
- keep controllers thin
- put business rules in services/domain layer
- persistence code belongs in repositories
- security must be explicit, never implicit
- every business query and write path must enforce tenant scoping
- tenant identity must come from authenticated backend context, not from arbitrary client input

## Desktop Rules
- `desktop` is a new application, not a copied continuation of `minifakt`
- prefer clean module boundaries from the start
- UI may reuse proven concepts from `minifakt`, but implementation should fit `ares`
- desktop should communicate with backend through explicit API clients
- avoid embedding persistence rules in desktop code
- Electron-specific code should stay isolated from domain/UI code

## Mobile Rules
- `mobile` is a first-class client, not an afterthought
- use React Native for the mobile application
- keep mobile architecture aligned with backend contracts
- do not re-implement backend business rules unless clearly required for offline UX
- mobile code should be organized by feature modules

## Shared Domain Expectations
The initial business domain will likely include:
- users
- companies
- company settings
- clients
- items/services
- invoices
- payments
- reports

Exact scope should be refined in `docs` plans before implementation.

## Documentation Rules
- all planning documents go into `docs`
- plans should be incremental and implementation-oriented
- prefer one master plan plus focused phase plans
- keep assumptions explicit
- record architecture decisions before major refactors

## Migration Guidance
When using the old `minifakt` project as reference:
- reuse ideas, not files by default
- compare workflows and domain concepts before copying structure
- copy code only when it clearly fits the new architecture
- remove SQLite/Electron-main-as-backend assumptions from the new design

## Coding Style
- keep code and comments in English
- prefer explicit types and explicit contracts
- keep files focused
- avoid premature abstraction
- choose boring, maintainable solutions over clever ones
- make cross-module boundaries obvious

## Decision Rules
- if a rule belongs to the business domain for all clients, it belongs in `server`
- if a rule exists only to support presentation or local interaction, it may belong in a client
- if a plan is unclear, document the decision in `docs` before broad implementation

## Initial Priority
Current priority is to define:
- root architecture
- planning documents in `docs`
- backend foundation in `server`
- future direction for the new desktop app in `desktop`

Implementation should follow the plans, not precede them.

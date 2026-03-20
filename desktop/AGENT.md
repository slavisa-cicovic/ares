# AGENT.md

## Scope
This document defines implementation rules for the `desktop` application in Ares.

The root [`AGENT.md`](/home/slavisa/devel/vs_code/ares/AGENT.md) still applies.
This file adds desktop-specific guidance.

## Purpose
`desktop` is the new Ares desktop client.

It is not a direct continuation of the old `minifakt` source.
The old project may be used as reference for:
- workflows
- user expectations
- screen ideas
- domain terminology

It must not be treated as the source of truth for architecture or persistence.

## Product Role
The desktop app is a client of the backend.

That means:
- `server` remains the source of truth for business data
- `desktop` consumes backend APIs
- desktop should not own business persistence
- desktop should not silently re-implement backend rules

## Architecture Direction
Desktop should follow a layered client structure:
- app shell
- feature modules
- API client layer
- auth/session layer
- UI components
- Electron-specific infrastructure

Recommended direction:
- isolate Electron runtime concerns from application feature code
- keep business feature code independent from Electron where possible
- prefer a thin bridge between renderer and native/Electron capabilities

## Backend Communication Rules
- desktop communicates with backend through explicit HTTP API clients
- backend base URL must come from configuration, not hardcoded constants spread through the app
- auth, token refresh, and logout behavior must be centralized
- no screen should manually implement raw authentication flow logic
- API error handling should be normalized in one place

Desktop should trust backend responses for:
- tenant scope
- authorization
- validation results
- business state transitions

## Auth Rules
- login must use the backend auth endpoints
- authenticated session state should be managed centrally
- token refresh should be automatic where practical
- logout must clear local auth state completely
- desktop must never infer tenant identity outside authenticated backend context

Recommended direction:
- keep auth/session logic in a dedicated module
- expose a single client-side auth state source to the rest of the app

## Persistence Rules
- no local SQLite source of truth
- no business repository layer backed by local desktop persistence
- local storage may be used only for client concerns such as:
  - session metadata
  - remembered environment/base URL if needed
  - non-sensitive UI preferences

If local caching is introduced later:
- it must be treated as cache, not authority
- it must not weaken tenant isolation

## Feature Module Rules
Desktop code should be organized by feature where possible.

Expected early feature areas:
- auth
- dashboard
- company
- users
- clients
- items
- invoices
- payments
- reports

Within each feature, prefer clear separation between:
- page/view code
- feature components
- API calls
- feature-specific state

## UI Rules
- `desktop` uses `MUI` as its standard component library
- desktop UI should preserve the practical strengths of the old app, not its technical constraints
- prioritize fast business workflows over decorative complexity
- forms should be explicit and validation feedback clear
- tables, filters, and detail views should map closely to backend contracts
- UI labels and messages should stay consistent with business terminology

Preferred direction:
- use `MUI` primitives first
- keep custom desktop wrappers thin and purposeful
- do not grow a parallel long-term custom component system for things `MUI` already solves well

## Theme Rules
- desktop theme should be organized as a structured MUI theme module, not a single growing styling file
- typography, shadows, and component overrides should be centralized under `desktop/src/theme`
- visual polish should come primarily from theme tokens and MUI overrides, not repeated ad hoc `sx` fragments
- presets may vary accent and surface direction, but must still share the same core typography, shadow system, and component behavior
- shell, dialogs, forms, cards, and tables should feel like one product language

Preferred direction:
- keep `createAppTheme.ts` as a composition layer
- define shared typography and shadow helpers
- add focused overrides for high-impact MUI components before introducing more local styling

## Electron Rules
- Electron-specific code must stay isolated from feature/UI code
- keep main-process responsibilities minimal
- avoid pushing business logic into Electron main
- preload should expose only deliberate, narrow bridges
- renderer should not receive broad native privileges by default

Recommended direction:
- keep the desktop app mostly as a secure frontend shell around backend APIs

## Security Rules
- do not store sensitive tokens in unsafe locations without explicit review
- do not expose backend secrets in renderer code
- do not trust client-side route guards as real authorization
- all privileged behavior must still be enforced by the backend

## State Management Rules
- keep server state and local UI state clearly separated
- avoid ad hoc duplicated fetching logic across screens
- prefer one consistent pattern for:
  - API calls
  - loading state
  - error state
  - refresh/invalidation

Do not introduce heavy state abstraction before real feature pressure appears.

## API Contract Rules
- desktop should adapt to backend contracts, not invent parallel domain models without reason
- avoid transforming API payloads into large alternate client-only models unless there is a clear UX gain
- pagination, filters, and sort parameters should mirror backend capabilities closely
- tenant identifiers should not be user-editable when they are derived from auth context

## Forms And Validation
- backend validation remains authoritative
- client-side validation should improve UX, not replace backend checks
- forms should surface field errors clearly and predictably
- conflict/business-rule errors from backend should be displayed in a user-meaningful way

## Networking Rules
- all HTTP configuration should be centralized
- retries must be deliberate, not automatic by default
- refresh-token logic should not create multiple competing refresh flows
- unauthorized responses should lead to a single consistent session handling path

## Reuse Rules
When reusing ideas from `minifakt`:
- prefer reusing flow and screen intent
- do not copy old persistence or IPC assumptions
- do not import old architectural constraints just because they were familiar

## Documentation Rules
- implementation plans for `desktop` belong in `docs`
- this file should hold persistent desktop guidance, not rollout tasks
- if desktop architecture changes materially, update this file before or with the implementation

## Initial Priority
The first desktop phases should likely establish:
- desktop stack and project bootstrap
- app shell and routing
- auth flow against backend
- API client foundation
- first dashboard/report screen

Desktop implementation should follow these foundations, not bypass them.

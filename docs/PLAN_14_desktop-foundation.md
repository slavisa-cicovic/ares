# PLAN_14_desktop-foundation.md

## Project
Ares

## Goal
Bootstrap the first usable Ares desktop application foundation on top of the existing backend.

---

## Summary

The backend now supports:

- authentication and token flow
- company and user management
- clients, items, invoices, and payments
- invoice payment state
- reports summary

The next step is to create the first real desktop client foundation that can:

- authenticate against the backend
- maintain session state
- call backend APIs through a shared client layer
- render the first authenticated application shell
- display the first useful business summary screen

This phase should produce a real starting point for desktop product development, not just a technical skeleton.

---

## Scope

This plan covers:

- desktop project bootstrap
- initial desktop architecture
- backend API client foundation
- auth/session flow
- app shell and navigation foundation
- first dashboard/report summary screen

This plan does not cover:

- full desktop feature implementation
- offline mode
- local business persistence
- advanced Electron-native integrations
- full settings or synchronization workflows

---

## Why This Comes Next

The backend now has enough business value to support a real client.

Continuing to add backend modules without a real client would delay feedback on:

- API usability
- auth/session ergonomics
- response shape quality
- dashboard usefulness

The desktop app should now become the first consumer that validates the backend as an actual product surface.

---

## Core Decisions

This phase should lock the following:

- desktop is a backend-connected client, not a local-data app
- auth/session handling is centralized
- API communication is centralized
- the first desktop milestone is a working authenticated shell
- the first meaningful authenticated screen is a dashboard summary

Recommended v1 direction:

- keep the first desktop slice narrow
- avoid prematurely implementing many modules
- prioritize a clean foundation over broad feature coverage

---

## Technology Direction

This phase should explicitly choose and bootstrap the desktop stack.

At minimum, it should define:

- Electron runtime choice
- frontend framework choice
- build/tooling choice
- package manager choice

Recommended practical direction:

- Electron for desktop shell
- React for renderer UI
- TypeScript
- Vite-based setup if it keeps startup simple

Reason:

- this gives a modern, maintainable desktop stack
- it aligns well with a backend-connected application model

---

## App Structure Direction

Recommended initial desktop structure:

- app bootstrap
- auth module
- api module
- layout/shell module
- dashboard module
- shared UI components
- Electron-specific infrastructure

The first implementation should keep:

- Electron code isolated
- renderer feature code framework-focused
- backend API details out of page components

---

## Backend API Client Direction

This phase should introduce a centralized API layer that handles:

- base URL configuration
- auth headers
- token refresh behavior
- standardized error handling

Recommended behavior:

- desktop does not scatter raw `fetch` calls across screens
- API client code is grouped and reusable
- auth failure handling is centralized

This layer should be designed so later feature modules can plug into it without rethinking session behavior.

---

## Auth And Session Direction

This phase should implement:

- login screen
- login call to backend
- token storage strategy
- automatic session hydration on app startup
- logout flow
- `/auth/me` bootstrap or equivalent authenticated identity loading

Recommended direction:

- keep auth state in one dedicated client-side module
- do not let each screen decide whether the user is authenticated
- refresh-token handling should be centralized and predictable

The first desktop foundation should make it possible to:

- open app
- log in
- restore session after restart if valid
- log out cleanly

---

## Navigation Direction

This phase should create:

- unauthenticated route/screen for login
- authenticated application shell
- initial navigation structure

Recommended initial authenticated areas:

- dashboard
- placeholder navigation entries for future modules if useful

The goal is not to implement all routes, but to establish the navigation model that future modules will extend.

---

## First Screen Direction

The first authenticated screen should be a dashboard summary powered by the backend reporting endpoint.

Recommended content:

- total invoiced amount
- total paid amount
- total outstanding amount
- unpaid / partially paid / paid invoice counts

Reason:

- this gives immediate business value
- it exercises auth, API client, reporting, and app shell in one coherent slice

---

## Configuration Direction

This phase should define how desktop gets backend configuration.

At minimum:

- backend base URL for development

Recommended direction:

- use environment-based configuration for development
- keep configuration centralized
- avoid hardcoding backend URLs deep in feature code

If needed, local development can point to:

- Spring Boot backend running locally

---

## Electron Security Direction

This phase should keep Electron responsibilities narrow.

Recommended rules:

- renderer should not receive broad native privileges
- preload bridge should stay small and explicit
- do not move backend business logic into Electron main
- desktop should mostly behave like a secure frontend shell

This keeps the app aligned with the backend-first architecture already defined.

---

## Persistence Direction

This phase should not introduce a local business database.

Allowed local persistence in this phase:

- session-related storage if needed
- environment preference if needed
- non-sensitive UI preferences if needed

Not allowed in this phase:

- local invoice/client/item source of truth
- local repository layer pretending to be business persistence

---

## UI Direction

This phase should favor:

- clarity
- practical workflow orientation
- fast auth-to-dashboard path

Recommended first UI deliverables:

- login form
- authenticated app shell
- dashboard cards/summary blocks
- loading and error states

The UI does not need to be fully branded yet, but it should already feel intentional and usable.

---

## Testing Expectations

This phase should include at least:

- basic desktop auth flow verification
- API client behavior verification where practical
- dashboard summary fetch verification

The exact test stack may depend on the chosen desktop tooling, but the phase should not rely only on manual clicking.

---

## Deliverables

- desktop project bootstrap exists
- app can connect to backend
- login works
- session state is managed centrally
- authenticated shell exists
- dashboard summary screen works against real backend endpoint

---

## Acceptance Criteria

- developer can start the desktop app locally
- desktop can authenticate against the local backend
- desktop can keep and use session state correctly
- desktop can fetch and display reports summary data
- desktop architecture is ready for additional feature modules

---

## Risks

- choosing too much desktop tooling at once could slow down delivery
- weak session handling would make all future modules unstable
- leaking Electron/runtime concerns into feature code would create long-term friction

---

## Next Step After This Plan

After this plan, the likely next step is one of:

- dashboard refinement
- client module in desktop
- invoice list/detail flow in desktop

The exact choice should depend on which workflow we want to make usable first.

---

## Assumptions

- desktop will use Electron
- backend remains locally available during development
- first desktop milestone should be narrow, stable, and backend-connected

---

## Notes

- This phase should validate the whole backend-first direction through a real client.
- The first win is not feature breadth, but a clean authenticated desktop foundation.

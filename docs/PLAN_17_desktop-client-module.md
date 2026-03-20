# PLAN_17_desktop-client-module.md

## Project
Ares

## Goal
Implement the first real business module in the desktop application by connecting the existing backend `client` module to a usable desktop workflow.

---

## Summary

The backend already supports a tenant-safe `client` module with:

- list
- detail
- create
- update
- soft delete
- search and pagination
- role-aware access

The desktop app now has enough shell structure to consume that module properly.

This phase should make `Clients` the first real desktop business area instead of a placeholder screen.

---

## Scope

This plan covers:

- desktop client list screen
- backend-connected search flow
- client create form
- client edit form
- client delete flow
- role-aware desktop behavior for read vs write actions

This plan does not cover:

- advanced client analytics
- bulk import/export
- invoice-client cross workflows
- mobile client implementation

---

## Why This Comes Next

The desktop foundation is now mature enough that adding another shell-only change would have low value.

The next meaningful step is to validate a full CRUD business flow in the desktop app.

`client` is the right first choice because:

- backend support already exists
- the model is simpler than invoices
- it will establish a reusable pattern for other desktop modules
- it will pressure-test API client organization, forms, tables, and permissions

---

## Core Decisions

This phase should lock the following:

- `Clients` becomes the first real desktop feature module
- the desktop client module should consume backend contracts directly
- tenant scope remains purely backend-derived
- company-admin can create, edit, and delete clients
- regular user can view clients but not mutate them

The desktop should reflect authorization clearly in the UI, but backend authorization remains authoritative.

---

## Desktop Module Direction

Recommended client module structure:

- client screen/view
- client API helper layer
- client form component
- client list/table component
- client-specific DTO mapping only where needed

The module should stay feature-scoped and not leak table/form logic into global app code.

---

## List Screen Direction

The desktop should implement a `Clients` list screen that supports:

- backend-backed list loading
- text search
- pagination
- empty state
- loading state
- error state

Expected fields in the first list view:

- name
- pib
- email
- phone
- status

The list should feel like a real work screen, not a placeholder container.

---

## Form Direction

The desktop should implement a client form for:

- create
- edit

Expected fields in the first form:

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

The form should:

- use the backend as validation authority
- still provide clear client-side UX feedback
- display backend validation or conflict errors cleanly

---

## Delete Direction

Delete should be implemented as a desktop confirmation flow over the existing backend soft delete endpoint.

Rules:

- delete action should be visible only where it makes sense
- destructive action should require a confirmation step
- after delete, list state should refresh predictably

The UI should not imply hard delete.

---

## Navigation And Shell Integration

This phase should replace the `Clients` placeholder entry with a real screen.

The module should fit into the existing shell without reworking the entire desktop navigation model.

The user should be able to:

- open `Clients`
- search
- create a new client
- edit an existing client
- delete a client if authorized

---

## Role-Aware UI Direction

Desktop should adapt visible actions based on current user roles.

Recommended behavior:

- `company-admin`
  - can see create/edit/delete actions
- `user`
  - can see list and details
  - cannot see or trigger write actions

This should improve UX, but backend remains the real enforcement layer.

---

## API Integration Direction

This phase should add client-specific API methods to the desktop API layer.

Expected backend integration:

- `GET /clients`
- `GET /clients/{id}`
- `POST /clients`
- `PUT /clients/{id}`
- `DELETE /clients/{id}`

The client module should reuse existing auth/session infrastructure and not introduce ad hoc fetch logic.

---

## Reusable Pattern Direction

This phase should intentionally produce a pattern that can be reused later for:

- `item`
- `invoice`
- `payment`

That means the implementation should establish a good baseline for:

- list page composition
- search/query state
- form workflow
- API error handling
- role-aware action rendering

---

## Acceptance Criteria

This plan is complete when:

- `Clients` is no longer a placeholder in desktop navigation
- desktop can list backend clients
- desktop can search backend clients
- company-admin can create and edit clients
- company-admin can soft delete clients
- regular users get a read-only clients experience
- the implementation establishes a reusable desktop feature pattern

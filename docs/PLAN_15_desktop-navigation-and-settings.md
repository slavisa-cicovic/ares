# PLAN_15_desktop-navigation-and-settings.md

## Project
Ares

## Goal
Replace the temporary session-focused desktop shell with a real application navigation model and introduce the first settings surfaces for company and user profile management.

---

## Summary

The current desktop foundation proves that:

- login works
- session state is restored
- the dashboard can load backend summary data

However, the current authenticated shell is still too narrow for the next product needs.

The desktop app now needs:

- a real application menu/navigation model
- a clear place for company-level settings
- a clear place for user-level settings
- a structure that can grow into future branding and template customization

This phase should replace the temporary account/session card mentality with a real application shell.

---

## Scope

This plan covers:

- desktop primary navigation
- authenticated shell restructuring
- `Settings` entry in navigation
- company settings screen foundation
- profile settings screen foundation
- appearance/settings placeholder direction

This plan does not cover:

- full client/item/invoice desktop modules
- file upload implementation for logo/avatar if backend is not ready
- PDF template editing
- advanced theming engine

---

## Why This Comes Next

The backend now already supports meaningful business flows.

The desktop app should now stop behaving like a demo shell and start behaving like a product shell.

Without this step:

- company management fields have no natural home in the UI
- user profile actions have no natural home in the UI
- future modules will be added into an unstable shell structure

This phase creates the UI structure that later modules will extend.

---

## Core Decisions

This phase should lock the following:

- authenticated desktop uses a persistent application menu, not a session card as the primary account surface
- company-level settings and user-level settings are separate surfaces
- `Settings` is a first-class area of the desktop app
- future branding, theme, and PDF template configuration will live under settings-related flows

Recommended initial top-level navigation:

- Dashboard
- Clients
- Items
- Invoices
- Payments
- Reports
- Settings

The first implementation may keep some entries as placeholders, but the shell structure should be real.

---

## Settings Direction

This phase should establish the first settings information architecture.

Recommended settings areas:

- Company
- Profile
- Appearance

Future settings areas:

- Invoice Templates
- Billing or subscription if needed later

The key separation is:

- company settings belong to the tenant/company
- profile settings belong to the authenticated user

They should not be merged into one generic account screen.

---

## Company Settings Direction

The desktop should prepare a company settings screen for company administrators.

Expected editable company fields:

- `name`
- `logo`
- `pib`
- `mb`
- `address`
- `email`
- `phone`
- `bankAccount`

For now, this phase should decide:

- how these fields are grouped in the UI
- how company-admin-only access is represented
- how future logo upload support will fit into the screen

If backend support for all fields is not yet ready, the UI may start with:

- implemented editable fields already supported by backend
- clearly structured placeholders for the rest

---

## Profile Settings Direction

The desktop should prepare a user profile screen for per-user settings.

Expected profile capabilities:

- change password
- set avatar

This phase should define:

- where profile editing lives in navigation
- how password change is presented
- how future avatar upload support will fit into the screen

If backend support is not yet ready for all profile actions, this phase may establish the screen structure first and implement only the supported slice.

---

## Appearance Direction

The desktop should prepare a place for theme selection.

This phase should support:

- a company or user-visible appearance section
- either theme selection from predefined themes
- or a placeholder structure if backend persistence is not ready yet

This decision must stay future-friendly for:

- predefined themes
- limited branding customization

This phase does not need to implement a full theme engine, but it should avoid a shell structure that blocks one later.

---

## Future PDF Template Direction

PDF template customization is not part of this phase.

However, this plan should explicitly reserve product structure for:

- invoice template selection
- invoice template customization
- brand-aware PDF output

This future capability should belong under settings-related flows, not be treated as an isolated technical feature.

---

## UI Structure Direction

This phase should restructure the authenticated shell so that:

- navigation is always visible and stable
- the current screen area is separate from session/account actions
- profile and logout actions move into a more natural account/menu area

Recommended direction:

- sidebar or structured desktop navigation rail
- top bar with current user/company context
- settings and account actions integrated into the shell instead of a standalone session card

The shell should start feeling like a desktop business application, not a proof-of-concept page.

---

## Backend Coordination

This phase may expose backend gaps.

Likely follow-up backend work:

- extend company update model for full company profile fields
- add password change endpoint
- add avatar/logo upload or asset reference strategy
- add theme preference persistence if needed

The desktop plan should therefore be implemented with clear boundaries between:

- current supported backend fields
- future backend-backed settings capabilities

---

## Acceptance Criteria

This plan is complete when:

- the authenticated shell uses real application navigation
- `Settings` exists as a real area of the app
- company settings have a defined screen structure
- user profile settings have a defined screen structure
- session/account actions are no longer centered around a temporary session card
- the shell is ready for later feature screens and settings expansion

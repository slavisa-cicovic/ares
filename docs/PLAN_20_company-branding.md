# PLAN_20 Company Branding

## Goal

Introduce `company branding` as a first-class tenant concept so that the desktop workspace clearly reflects which company the user is currently working for.

This plan replaces the idea of purely local user theme preference with backend-backed tenant branding.

## Problem

The current desktop theme preference is stored locally and selected per device.

That is not enough for the real usage model:

- one accountant may work with multiple companies
- the same person may log into different company accounts during the day
- header text alone is not a strong enough company signal
- local theme choice can blur tenant identity instead of reinforcing it

Because of that, the UI should make company context obvious at all times.

## Target Direction

The active company should define the branding baseline for the desktop workspace.

That means:

- the workspace should visually signal which company is active
- company branding must come from backend company settings
- branding should apply consistently after login and account switch
- local user preference should not override tenant identity

## Scope

This plan covers:

- backend company branding fields
- extending company settings APIs
- desktop branding model and application
- replacing local theme preference as the main theme source
- shell-level visual reinforcement of active company context

This plan does not cover:

- full logo upload infrastructure if that requires a separate asset subsystem
- PDF invoice template branding
- custom brand designer UI

## Branding Model

The initial company branding model should stay focused.

Recommended fields:

- `brandColor`
- `themePreset`
- `logoUrl` or `logoAssetKey` placeholder

Optional later fields:

- `brandSecondaryColor`
- `workspaceBadgeText`
- `invoiceTemplateId`

The first version should keep the branding surface small and stable.

## Theme Ownership

The primary desktop theme source should become company branding.

That means:

- company branding determines the main workspace visual identity
- local user theme preference should be removed or reduced to a very minor future concern
- desktop should apply the active company's branding immediately after session restore or login

If user-level appearance preferences return later, they must not hide or weaken company identity.

## Backend Changes

The backend company model should be expanded to support branding.

Expected additions:

- branding fields in the company entity
- migration for new branding columns
- `GET /company` returns branding data
- `PUT /company` allows `company-admin` to update branding fields

Validation direction:

- `brandColor` should be validated as a supported color format
- `themePreset` should be validated against allowed preset identifiers
- branding updates remain tenant-scoped like the rest of company settings

## Desktop Changes

Desktop should stop treating theme as only local state.

Expected direction:

- active company branding is loaded from the authenticated company payload
- shell and theme provider derive the active visual identity from company branding
- current local theme storage should be removed or deprecated
- branding changes in settings should immediately update the visible workspace

## Shell Impact

The shell should become a stronger company identity surface.

Branding should visibly affect:

- logo area
- company badge / avatar block
- drawer accent
- selected navigation state
- primary action emphasis
- top-level workspace tone

The user should be able to notice at a glance that they are in company `A`, not company `B`.

## Settings Impact

Company settings should grow a branding subsection.

Expected early branding controls:

- theme preset selection
- brand color
- logo placeholder field or upload placeholder

This section should belong to company settings, not personal profile settings.

## Migration Direction

The existing local theme preference should be phased out.

Recommended order:

1. Add backend branding fields and expose them through company APIs.
2. Load company branding into desktop auth/session state.
3. Make desktop theme derive from company branding.
4. Remove or retire local-only theme preference controls.

## Acceptance Criteria

This plan is complete when:

- company branding exists in backend company settings
- desktop theme is derived from active company branding
- switching company account clearly changes workspace identity
- the shell makes active company context visually obvious
- local user theme preference is no longer the main source of workspace appearance

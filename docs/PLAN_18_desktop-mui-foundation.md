# PLAN_18_desktop-mui-foundation.md

## Goal
Adopt `MUI` as the standard UI foundation for the `desktop` application.

The goal is not to partially sprinkle `MUI` into the current desktop codebase.
The goal is to explicitly standardize the desktop UI layer around `MUI` so that:
- forms
- dialogs
- tables
- navigation
- feedback states
- layout primitives

follow one coherent component model.

This plan exists because the current custom desktop UI foundation is functional, but the UX quality and consistency are below the target for a long-lived business desktop application.

## Why This Change
The current desktop implementation already has:
- a backend-integrated app shell
- auth/session flow
- settings screens
- a first client list and dialog flow

But it also already shows the downside of continuing with a fully custom UI layer:
- slower iteration on standard CRUD screens
- more manual CSS work for basic interactions
- more opportunities for inconsistent form, dialog, and table behavior
- weaker default accessibility and state handling than a mature component library

For this project, `MUI` is a better fit than `Tailwind` because:
- the desktop app is a business CRUD-heavy application
- it needs strong dialog, form, table, menu, and navigation primitives
- the team is already familiar with `MUI`
- it shortens the path to a stable and professional admin/business UX

## Decision
`desktop` should use `MUI` as its standard component library.

That means:
- new desktop screens should be built on `MUI`
- core reusable UI primitives should be `MUI`-based
- the current custom CSS/component layer should be reduced to:
  - app-specific wrappers
  - theme tokens
  - small layout glue
  - feature-specific styling where `MUI` alone is not enough

This does **not** mean:
- copy `minifakt` source blindly
- rebuild the desktop app in one large rewrite
- keep two parallel long-term UI systems in active use

## Scope
This plan covers the `desktop` UI foundation only.

It includes:
- `MUI` package introduction
- theme setup
- app-level providers
- dialog/form/table/navigation direction
- migration path for the current desktop screens

It does not include:
- mobile UI
- backend API changes
- PDF generation work
- invoice template customization
- a full visual redesign of every screen in one pass

## Target Direction
Desktop should move toward this UI structure:

- `app`
  - MUI theme provider
  - app shell layout
  - navigation frame
- `components`
  - thin reusable wrappers over `MUI` where useful
  - standardized dialogs
  - standardized table helpers
  - standardized form helpers
- `features`
  - feature screens built from `MUI` components and desktop wrappers

Recommended principle:
- prefer `MUI` primitives first
- add custom wrappers only where they improve consistency or reduce repetition

## Component Strategy
### Dialogs
`CustomDialog` should either:
- become a thin wrapper over `MUI Dialog`, or
- be replaced by direct `MUI Dialog` usage plus a smaller shared wrapper

The target behavior should include:
- consistent title/content/actions structure
- escape close
- backdrop close
- proper focus management
- responsive sizing

### Forms
`react-hook-form` and `zod` remain valid and should stay.

The form direction should become:
- `react-hook-form`
- `zod`
- `@hookform/resolvers`
- `MUI` field components, likely through RHF wrappers

The current custom `hook-form` layer should be evaluated as follows:
- if a wrapper maps cleanly to `MUI`, keep it and re-implement it over `MUI`
- if a wrapper adds no real value, remove it

Expected retained wrappers:
- `FormProvider`
- `RHFTextField`
- `RHFSelect`
- `RHFSwitch`
- `RHFNumberField`
- `RHFCurrencyField`
- `RHFDatePicker`
- possibly `RHFLogoPicker`

### Tables
The current custom table layer should not remain the long-term foundation.

The target should be either:
- `MUI Table` with shared helper wrappers, or
- `MUI Data Grid` if later feature pressure justifies it

For the current phase, the safer direction is:
- use `MUI Table`
- keep desktop-specific pagination/sort helper patterns where useful
- avoid introducing `Data Grid` prematurely

### Navigation And Layout
The app shell should move toward `MUI` layout primitives:
- `Drawer`
- `AppBar`
- `Toolbar`
- `Container`
- `Stack`
- `Box`
- `Paper`

The current shell structure can remain conceptually the same, but implementation should become `MUI`-based.

### Feedback And Status
Use `MUI` for:
- `Alert`
- `Snackbar`
- `Chip`
- loading placeholders where appropriate
- buttons and empty states

## Styling Strategy
The desktop app should stop relying on large ad hoc global CSS as the main UI mechanism.

Preferred direction:
- use `MUI` theme tokens first
- use `sx` for local component styling where appropriate
- use small shared style helpers for repeated patterns
- keep global CSS only for truly global concerns

Global CSS should become smaller over time, not larger.

## Theme Strategy
The desktop app should define a single `MUI` theme foundation that covers:
- palette
- typography
- shape
- spacing
- component defaults

The previously introduced theme idea can survive, but should be expressed through `MUI` theme configuration, not parallel handcrafted CSS themes.

Initial recommendation:
- one strong default theme first
- postpone multiple end-user themes until the main UI system is stable

## Migration Strategy
Migration should be incremental, not a rewrite.

Recommended order:

1. Add `MUI` dependencies and app-level theme/provider setup
2. Convert dialog foundation to `MUI Dialog`
3. Convert form wrappers to `MUI`-based RHF wrappers
4. Convert `Clients` list and `ClientFormDialog`
5. Convert `Settings`
6. Convert `Dashboard`
7. Convert later modules (`Items`, `Invoices`, `Payments`, `Reports`) as they are built

## Existing Custom Components
Current custom desktop components should be classified into three groups.

### Keep concept, re-implement on `MUI`
- dialog wrapper
- hook-form wrappers
- maybe small table helpers

### Keep as app-specific helpers
- auth/session providers
- API client layer
- feature-level state and orchestration

### Phase out
- custom CSS-heavy base UI primitives that duplicate what `MUI` already solves well

## Risks
### Mixed UI system risk
If migration is partial and unmanaged, the app can end up with:
- half custom CSS
- half `MUI`
- duplicated component responsibilities

This should be avoided by choosing `MUI` as the clear target and migrating screen-by-screen.

### Wrapper overengineering risk
If every `MUI` component gets a custom wrapper immediately, migration slows down and clarity gets worse.

Wrappers should exist only when they provide:
- stable app conventions
- reduced duplication
- cleaner feature code

### Theme drift risk
If old CSS tokens and new `MUI` theme tokens diverge, the visual system will split.

The `MUI` theme should become the primary visual source of truth.

## Implementation Notes
When the implementation starts:
- update `desktop/AGENT.md` to state that `desktop` uses `MUI`
- do not keep investing in old custom primitive styling
- prefer converting existing dialog/form/table slices before adding more custom UI foundation

## Acceptance Criteria
- `desktop` has `MUI` installed and configured at app root
- a shared `MUI` theme exists
- dialogs use `MUI Dialog`
- form wrappers are `MUI`-based
- `Clients` screen and `ClientFormDialog` are migrated to `MUI`
- global CSS usage is reduced rather than expanded
- the desktop UI direction is explicit and no longer ambiguous

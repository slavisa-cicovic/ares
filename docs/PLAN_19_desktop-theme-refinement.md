# PLAN_19 Desktop Theme Refinement

## Goal

Refine the `desktop` visual system so that it feels more professional, consistent, and mature while staying aligned with the current MUI-based direction.

This plan does not aim to copy another project's theme. It uses the theme structure from `tms/web/src/theme` as a reference for how to organize and scale the desktop design system.

## Scope

This plan covers:

- refactoring the desktop theme into a structured theme module
- introducing a clearer typography system
- introducing centralized shadows and custom shadows
- adding focused MUI component overrides
- improving the visual baseline for shell, tables, dialogs, forms, and action components

This plan does not cover:

- new business features
- PDF styling
- mobile theme work
- full brand customization

## Current Problem

The current desktop UI is functional, but the visual system is still thin:

- theme logic is too concentrated in a single file
- component styling is inconsistent across screens
- spacing, elevation, border, and typography decisions are not centralized enough
- MUI is present, but we are not yet using its theme override system deeply enough

Because of that, the UI can feel improvised instead of deliberate.

## Target Direction

The desktop app should move toward a structured MUI theme system with these characteristics:

- typography scale is defined once and reused everywhere
- shadows and elevation are consistent
- cards, dialogs, tables, and fields share a coherent visual language
- component behavior is standardized through theme overrides instead of local ad hoc styling
- theme presets remain possible, but the system underneath them becomes stronger

## Theme Structure

The `desktop/src/theme` area should evolve into something like:

- `createAppTheme.ts`
- `typography.ts`
- `shadows.ts`
- `custom-shadows.ts`
- `overrides/index.ts`
- `overrides/button.ts`
- `overrides/paper.ts`
- `overrides/card.ts`
- `overrides/dialog.ts`
- `overrides/text-field.ts`
- `overrides/table.ts`
- `overrides/pagination.ts`

`createAppTheme.ts` should become a composition layer, not a large styling file.

## Typography

Typography should be tightened and made more intentional:

- define a shared `pxToRem` helper
- define a clear heading and body scale
- use consistent weights for label, body, title, and section hierarchy
- avoid default-looking MUI text sizing
- keep button text readable and avoid aggressive uppercase styling by default

Typography should improve:

- page hierarchy
- dialog clarity
- table readability
- form label consistency

## Shadows And Surface Language

The desktop app should define:

- base shadow set
- custom semantic shadows for key component states

The visual goal is:

- cleaner cards
- calmer panels
- stronger but controlled action emphasis
- better separation between shell background and content surfaces

We should avoid both extremes:

- flat lifeless UI
- overly glossy or noisy enterprise UI

## Priority Component Overrides

The first override pass should focus on the components that shape most of the desktop UX:

- `MuiButton`
- `MuiPaper`
- `MuiCard`
- `MuiDialog`
- `MuiTextField`
- `MuiFormHelperText`
- `MuiTableCell`
- `MuiTableHead`
- `MuiTablePagination`
- `MuiDrawer`
- `MuiAppBar`

These overrides should standardize:

- border radius
- padding rhythm
- elevation usage
- hover and focus feedback
- header treatment
- helper text spacing
- table density and readability

## Presets

The current preset idea can remain, but presets should become thinner.

A preset should mainly define:

- primary color family
- accent color direction
- small surface color variations

A preset should not bypass the shared design system.

That means all presets must still use the same:

- typography
- shadow system
- component overrides
- spacing logic

## Shell Impact

The shell should benefit from the refined theme without requiring major layout rewrites.

The refinement should improve:

- drawer feel
- app bar clarity
- account panel polish
- page header hierarchy
- content card presentation

The shell should feel more like a product workspace and less like a collection of styled boxes.

## Feature Screen Impact

The first screen to visibly benefit should be `Clients`.

Expected improvements:

- stronger table hierarchy
- cleaner search/filter controls
- more professional dialog presentation
- more stable form rhythm
- more polished action button states

After that, the same system should be reused for `Items`, `Invoices`, and `Settings`.

## Implementation Order

1. Refactor `desktop/src/theme` into a structured module layout.
2. Introduce typography and shadow helpers.
3. Add shared component overrides for surfaces, forms, dialogs, and tables.
4. Reduce ad hoc local styling in `Shell`, `ClientsScreen`, and `ClientFormDialog`.
5. Validate that the refined theme works across all current desktop screens.

## Acceptance Criteria

This plan is complete when:

- desktop theme is no longer centered in a single styling file
- typography, shadows, and overrides are centralized
- `Shell`, `ClientsScreen`, and `ClientFormDialog` visibly benefit from the refined theme
- local component styling is reduced where theme overrides should own the behavior
- desktop UI feels more cohesive and professional without copying another project directly

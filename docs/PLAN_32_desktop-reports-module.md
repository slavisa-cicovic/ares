# PLAN_32

## Goal

Replace the desktop `Reports` placeholder with a real reporting screen that uses the existing backend summary endpoint and the shared date-range filter.

## Scope

- `Reports` must no longer be a placeholder in desktop navigation
- add a real `Reports` screen backed by `GET /reports/summary`
- support reporting by date range using the shared `DateRangeFilter`
- present the current summary metrics in a clear accountant-friendly layout

## Functional requirements

### 1. Summary screen

Add a `Reports` screen with summary cards for:

- `Invoices`
- `Total Invoiced`
- `Total Paid`
- `Total Refunded`
- `Outstanding`
- `Unsettled Refund`

### 2. Status breakdown

Display invoice payment breakdown:

- `Unpaid`
- `Partially Paid`
- `Paid`

This can be shown as:

- compact stat cards
- or a secondary summary row beneath the main metrics

### 3. Date range filtering

Use the shared:

- `desktop/src/components/filters/DateRangeFilter.tsx`

Support:

- preset-based filtering
- `custom` date range
- no date filter by default

The screen should request:

- `GET /reports/summary`
- with optional `from` and `to`

### 4. Error and empty behavior

- invalid custom date range should not send a request
- backend errors should show a clear error alert
- loading state should be visible and stable

## Technical design

### Desktop API

Extend the desktop API layer so `getReportsSummary` accepts optional:

- `from`
- `to`

Files:

- `desktop/src/lib/api/DesktopApiClient.ts`
- `desktop/src/features/auth/AuthProvider.tsx`
- `desktop/src/lib/api/types/reports.ts` if needed

### Desktop screen

Add:

- `desktop/src/features/reports/ReportsScreen.tsx`

Integrate into:

- `desktop/src/app/App.tsx`

### Shared patterns

Reuse:

- MUI cards and stacks from the current theme system
- `DateRangeFilter`
- existing `ApiClientError` handling patterns from `Invoices` and `Payments`

## UX rules

- the screen should read clearly at a glance for bookkeeping use
- money values must use the same desktop number formatting as the rest of the app
- summary cards should not feel like raw debug output
- keep the first version focused and clean; do not overload it with charts yet

## Verification

### Desktop

- `npm run build`
- manually verify:
  - opening `Reports`
  - switching presets
  - custom date range
  - invalid custom range
  - summary numbers updating after filter changes

### Backend

No backend implementation is required if the existing summary endpoint is sufficient.

If any backend change becomes necessary:

- run relevant integration tests
- confirm reports remain tenant-safe

## Out of scope

Do not implement yet:

- charts
- CSV export
- PDF report export
- advanced report drill-down
- aging reports
- tax/VAT specific reporting views

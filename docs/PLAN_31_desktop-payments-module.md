# PLAN_31

## Goal

Add a proper `Payments` desktop module as a central payment overview, instead of keeping payment flows only inside the invoice detail dialog.

## Scope

- `Payments` must no longer be a placeholder in desktop navigation
- add a payments list screen with backend integration
- support company-scoped payment browsing with paging and filters
- connect each payment row to its invoice and client
- keep the existing payment-create flow from the invoice detail dialog as a complement, not a replacement for the payments module

## Backend assumptions

- the backend already has a `Payment` entity and create flow
- the backend already computes invoice payment state (`paidAmount`, `outstandingAmount`, `paymentStatus`)
- if a payment list/read endpoint does not exist yet, it should be added as part of this plan

## Functional requirements

### 1. Payments list

Add a desktop screen:

- route/view: `Payments`
- payment table with columns:
  - `Payment Date`
  - `Invoice`
  - `Client`
  - `Amount`
  - `Note`
  - `Actions`

### 2. Filters

Support at minimum:

- search by:
  - invoice number
  - client name
  - note
- `from` / `to` date filter by `paymentDate`

If the backend already returns sufficient data, search should be server-driven like in the other modules.

### 3. Payment detail

Add a read-only payment detail dialog or side panel showing:

- payment date
- amount
- note
- invoice number
- invoice issue date
- client name
- company currency

### 4. Create payment entry point

For now, keep the main payment-create flow on the invoice detail dialog.

Optionally, from the `Payments` list support:

- `Add Payment`
  - but only if the user first chooses a target invoice

If that makes the first iteration too broad, defer it.

### 5. Role behavior

- `company-admin`
  - can view the payments list
  - can add payments through the existing invoice flow
- regular `user`
  - read-only payments view

For the first step, payment edit/delete is not required.

## Technical design

### Shared date range filter

Since the same pattern already exists on `Invoices`, and will be needed on `Payments`, this plan should introduce a shared desktop component:

- `desktop/src/components/filters/DateRangeFilter.tsx`

The component should cover:

- `From`
- `To`
- compact MUI layout
- optional support for future presets:
  - `This month`
  - `Last month`
  - `This year`
  - `Custom`

For the first step, the component does not need to own filter state. State and range validation should remain in the screen that uses it, while the component standardizes the UI and props surface.

Use the component immediately in:

- `InvoicesScreen.tsx`
- `PaymentsScreen.tsx`

### Desktop API

Extend:

- `desktop/src/lib/api/types/`
  - add `payment.ts` if it does not already exist, or extend it with read models
- `desktop/src/lib/api/DesktopApiClient.ts`
  - list/get payments methods
- `desktop/src/features/auth/AuthProvider.tsx`
  - expose payments API methods through the auth context

### Desktop screen

Add:

- `desktop/src/features/payments/PaymentsScreen.tsx`

Follow the existing patterns from:

- `ClientsScreen.tsx`
- `ItemsScreen.tsx`
- `InvoicesScreen.tsx`

Use:

- MUI table
- `FiltersResult`
- the existing `useTable`

### Optional dialogs

As needed:

- `PaymentDetailDialog.tsx`

## Backend work if needed

If the backend does not yet have a payments list/read endpoint, add:

- `GET /payments`
- optionally `GET /payments/{id}`

Filter params:

- `page`
- `size`
- `sort`
- `search`
- `from`
- `to`

The read response should include enough data for the desktop table:

- payment id
- payment date
- amount
- note
- invoice id
- invoice number
- client id
- client name
- currency

## Validation and UX rules

- invalid date ranges must not trigger a request
- clear/reset filters should work like on `Invoices`
- amount formatting must stay consistent with the rest of the desktop app
- the payments list must remain tenant-safe

## Verification

### Backend

- integration test for the payment list endpoint, if added
- tenant safety
- date filter behavior
- invalid date range -> `400`

### Desktop

- `npm run build`
- manually verify:
  - opening `Payments`
  - search
  - date filters
  - paging
  - detail opening
  - read-only behavior for regular users

## Out of scope

Do not implement yet:

- edit payment
- delete/reverse payment
- refund list as a separate module
- payment import/bank statement reconciliation
- advanced payment analytics

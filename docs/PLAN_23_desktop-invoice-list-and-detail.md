# PLAN_23 Desktop Invoice List And Detail

## Goal

Implement the first desktop invoice workspace as a read-first slice:

- invoice list
- invoice filters
- invoice detail view

This phase should make invoices visible and usable from the desktop app before we move on to full draft editing.

## Why This Order

Invoices are more complex than:

- clients
- items

They already depend on:

- clients
- items
- lifecycle status
- payment status

Because of that, the first desktop invoice phase should prioritize visibility and correctness over early editing complexity.

List and detail first gives us:

- immediate value for browsing invoices
- validation of backend response shape
- a stable base for later draft editor work

## Scope

This plan covers:

- `Invoices` desktop screen
- invoice list from backend
- search and filter controls
- pagination and sorting
- invoice detail dialog or side panel
- role-aware behavior for browsing

This plan does not cover:

- draft invoice create flow
- line editor UX
- issue action
- payment registration
- PDF generation

## Backend Contract Assumption

The desktop app will use the already existing backend invoice endpoints for list and detail:

- `GET /invoices`
- `GET /invoices/{id}`

Expected backend fields include at least:

- `id`
- `invoiceNumber`
- `status`
- `paymentStatus`
- `issueDate`
- `dueDate`
- `clientId`
- `clientName`
- `subtotal`
- `taxTotal`
- `total`
- `paidAmount`
- `outstandingAmount`
- line items for detail view

If the current detail response is missing a field needed for a useful desktop detail panel, backend can be extended only where justified by actual UI need.

## Desktop UX Direction

The `Invoices` workspace should follow the same quality bar already set by:

- `Clients`
- `Items`

But invoice detail should feel slightly more structured because invoices are aggregates, not simple master-data rows.

Recommended desktop flow:

- user opens `Invoices`
- sees paged invoice list
- can filter and search
- selects or opens one invoice
- sees detail view with header and line items

## Invoice List

The list should support:

- server paging
- sorting where backend supports it
- search
- invoice status filter
- payment status filter

Useful visible columns for v1:

- `invoiceNumber`
- `clientName`
- `issueDate`
- `dueDate`
- `status`
- `paymentStatus`
- `total`
- `outstandingAmount`

## Invoice Detail

The detail view should show:

- invoice identification
- client summary
- dates
- invoice status
- payment status
- note if present
- line items
- totals block

Line item detail should include:

- line number
- item name snapshot
- item code snapshot if present
- quantity
- unit
- unit price
- tax rate
- line total

## Role Behavior

For this phase:

- `company-admin` can browse
- regular `user` can browse

There are no write actions yet in this plan, so role-based UI differences should be minimal.

## Desktop API Layer

The desktop API client should be extended with invoice-specific read methods:

- `getInvoices`
- `getInvoice`

Invoice DTO types should be introduced under:

- `desktop/src/lib/api/types/invoice.ts`

Avoid adding invoice types into unrelated domain type files.

## UI Structure

Expected implementation slices:

- `desktop/src/features/invoices/InvoicesScreen.tsx`
- `desktop/src/features/invoices/InvoiceDetailDialog.tsx`

Optional helper file if needed:

- `desktop/src/features/invoices/utils.ts`

## Visual Direction

The invoice workspace should use the current desktop MUI foundation.

The detail surface should feel more document-like than clients/items, but should still remain inside the same app design system.

Avoid:

- raw JSON-like rendering
- overly dense accounting tables without spacing
- ad hoc CSS outside the current MUI/theme approach

## Testing And Verification

Minimum verification for this plan:

- desktop build passes
- invoice list loads from backend
- filters and paging work
- invoice detail opens and shows line items plus totals

Verification command:

```bash
cd /home/slavisa/devel/vs_code/ares/desktop
npm run build
```

## Acceptance Criteria

This plan is complete when:

- `Invoices` is no longer a placeholder in desktop
- a real invoice list is visible
- invoice detail can be opened from the list
- users can understand invoice status, payment status, totals, and line structure from the desktop app

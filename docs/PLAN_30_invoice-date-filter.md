# PLAN_30 Invoice Date Filter

## Goal
- add invoice list filtering by date range
- make invoice browsing usable for real accounting periods

## Scope
- extend `GET /invoices` with:
  - `from`
  - `to`
- apply date filtering by `issueDate`
- add desktop filters on invoice list

## Backend
- update invoice list controller to accept optional `from` and `to`
- validate:
  - both are optional
  - if both exist, `from <= to`
- apply filtering on `issueDate`
- preserve existing tenant isolation and existing filters:
  - `search`
  - `status`
  - `paymentStatus`

## Desktop
- add two date inputs on [`InvoicesScreen.tsx`](/home/slavisa/devel/vs_code/ares/desktop/src/features/invoices/InvoicesScreen.tsx):
  - `From`
  - `To`
- changing either filter resets paging to first page
- filters combine with existing:
  - search
  - status
  - payment status

## UX Rules
- empty `from`/`to` means no date restriction
- invalid range should not hit backend silently
- show a small inline error or prevent request when `from > to`

## Tests
- backend integration tests for:
  - only `from`
  - only `to`
  - `from` + `to`
  - invalid range
- desktop verification:
  - list updates correctly
  - pagination resets on filter change

## Out Of Scope
- quick presets like:
  - `This month`
  - `Last month`
  - `This year`
- filtering by `dueDate`
- saved filters

## Expected Outcome
- invoice list becomes practical for monthly and period-based work
- users can isolate invoice batches without relying only on search and status filters

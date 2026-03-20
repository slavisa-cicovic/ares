# PLAN_24

## Goal

Implement a real desktop draft invoice editor on top of the existing backend invoice draft API.

## Scope

- replace the current read-only invoice flow with draft create/edit capability
- keep `Invoices` list and detail from `PLAN_23`
- support only draft authoring in this phase
- do not implement issue/payment actions here

## Desktop UX

- `Invoices` screen gets:
  - `New Invoice` action
  - `Edit` action for `DRAFT` invoices
  - no edit action for `ISSUED` invoices
- invoice editing happens in a dialog
- dialog contains:
  - invoice header section
  - invoice lines editor
  - totals summary
  - `Save draft`
  - `Cancel`

## Header Fields

- `client`
- `issueDate`
- `dueDate`
- `currency`
- `note`

## Lines Editor

- multiple invoice lines
- add line
- remove line
- reorder is not required in this phase
- each line supports:
  - optional `item`
  - `itemName`
  - `itemCode`
  - `unit`
  - `quantity`
  - `unitPrice`
  - `taxRate`

## Behavior Rules

- totals are backend-owned
- desktop may show line/totals preview, but backend response is source of truth
- `company-admin` can create and edit drafts
- regular `user` is read-only for invoices
- `ISSUED` invoices are read-only in desktop

## API Integration

- use existing backend endpoints:
  - `POST /invoices`
  - `PUT /invoices/{id}`
  - `GET /invoices/{id}`
- desktop also needs lookup data for:
  - clients
  - items

## Validation

- use `zod`
- require at least one line
- require valid client
- require `issueDate`
- require `dueDate`
- require line `itemName`
- require line `unit`
- require positive `quantity`
- require non-negative `unitPrice`
- require non-negative `taxRate`
- require `dueDate >= issueDate`

## Technical Slice

- add desktop invoice draft types
- add invoice upsert payload mapping in desktop API client
- add invoice draft schema
- add invoice form dialog
- add line row editor component if needed

## Done When

- user can create a new draft invoice from desktop
- user can edit an existing `DRAFT` invoice
- desktop refreshes list/detail after save
- `ISSUED` invoices stay read-only
- desktop build passes

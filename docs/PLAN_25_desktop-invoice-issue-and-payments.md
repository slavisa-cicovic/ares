# PLAN_25

## Goal

Complete the desktop invoice flow after draft editing by adding invoice issue and payment registration.

## Scope

- issue a `DRAFT` invoice from desktop
- register payments for `ISSUED` invoices
- show payment history in invoice detail
- keep `DRAFT` editing from `PLAN_24`

## Desktop UX

- `Invoices` list and detail should expose:
  - `Issue` action for `DRAFT` invoices
  - `Add Payment` action for `ISSUED` invoices
- payment entry happens in a dialog
- invoice detail should show:
  - payment history
  - current `paidAmount`
  - current `outstandingAmount`

## Issue Flow

- only `company-admin` can issue invoices
- `Issue` is available only for `DRAFT`
- after issue:
  - invoice becomes read-only
  - desktop refreshes list and detail
  - status changes to `ISSUED`

## Payment Flow

- payments are allowed only for `ISSUED` invoices
- payment dialog fields:
  - `paymentDate`
  - `amount`
  - `note`
- desktop should display validation and backend error messages
- desktop should refresh invoice detail and list after successful payment

## Payment History View

- show payment rows with:
  - `paymentDate`
  - `amount`
  - `note`
- newest payments first if backend already returns that order

## API Integration

- use existing backend endpoints:
  - `POST /invoices/{id}/issue`
  - `GET /invoices/{id}`
  - `GET /invoices/{id}/payments`
  - `POST /invoices/{id}/payments`

## Role Rules

- `company-admin`
  - can issue invoices
  - can register payments
- regular `user`
  - read-only access to invoice and payment data
  - no issue action
  - no payment action

## Validation

- payment amount must be positive
- payment date is required
- note is optional
- desktop relies on backend for overpayment protection and final business validation

## Technical Slice

- add payment API types to desktop
- extend invoice detail model if needed
- add payment dialog
- extend invoice detail dialog with payment history
- add issue action wiring in invoice list/detail

## Done When

- `company-admin` can issue a draft invoice from desktop
- `company-admin` can add payments to an issued invoice
- invoice detail shows payment history and updated totals
- regular `user` remains read-only
- desktop build passes

# PLAN_27

## Goal

Add refund / settlement handling for credit notes so the financial outcome is explicit after invoice reversal.

## Why

- a credit note alone reverses the document, but not necessarily the money flow
- fully paid invoices need an explicit refund or settlement outcome
- partially paid invoices need clear rules for remaining receivable vs refundable amount
- reports and desktop UX should show whether a credit note is only issued or also financially settled

## Model Direction

- keep credit note as a separate accounting document
- introduce explicit refund / settlement records or status, not implicit assumptions
- do not overload regular invoice payment records with reverse-money semantics unless the model stays clean

## Core Scenarios

- original invoice unpaid:
  - credit note should reduce or nullify receivable
  - no refund is needed
- original invoice partially paid:
  - part of the credit note may offset open receivable
  - remaining part may require refund
- original invoice fully paid:
  - credit note should usually create a refundable balance

## Backend Scope

- define refund / settlement model for credit notes
- decide whether the new concept is:
  - `credit_note_refunds`
  - or generic invoice settlements
- add API endpoints to:
  - inspect settlement state
  - record refund / settlement action
- expose read-model fields such as:
  - refundable amount
  - settled amount
  - remaining refund amount
  - settlement status

## Business Rules

- only `company-admin` can record refund / settlement actions
- settlement amount must not exceed remaining refundable amount
- refund / settlement must remain tenant-safe
- original invoice and credit note relationship must remain immutable

## Reporting Impact

- summary totals must remain coherent after both credit note creation and refund settlement
- reports should clearly distinguish:
  - net invoiced
  - net paid
  - refundable / unsettled credit note exposure

## Desktop Scope

- invoice detail should show refund / settlement state for credit notes
- desktop should allow `Record Refund` or equivalent only when there is remaining refundable amount
- users should see whether a credit note is:
  - issued only
  - partially settled
  - fully settled

## Technical Slice

- schema migration for refund / settlement records
- backend service and endpoint(s)
- integration tests for:
  - unpaid original
  - partially paid original
  - fully paid original
  - over-refund rejection
  - cross-tenant denial
- desktop detail/action wiring

## Done When

- credit notes have explicit financial settlement handling
- refund / settlement state is visible in API and desktop
- invalid refund scenarios are blocked
- reports stay financially coherent

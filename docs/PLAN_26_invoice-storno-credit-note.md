# PLAN_26

## Goal

Add storno / credit-note support for issued invoices instead of a simplistic cancel-only model.

## Why

- issued invoices must remain auditable
- accounting correction flow should preserve the original invoice
- business users need a formal way to reverse or reduce an already issued invoice

## Model Direction

- do not physically delete or overwrite an issued invoice
- do not use only a naive `CANCELLED` status as the primary correction mechanism
- introduce a dedicated storno / credit-note document linked to the original invoice

## Core Rules

- original issued invoice remains immutable
- storno document references the original invoice
- storno document belongs to the same company as the original invoice
- cross-tenant storno access must remain impossible
- storno can only be created from an `ISSUED` invoice

## Backend Scope

- extend invoice domain to support storno / credit-note linkage
- add fields such as:
  - document type
  - original invoice reference
- add endpoint to create storno / credit-note from an issued invoice
- add read model fields so desktop can show:
  - whether invoice has storno
  - linked original invoice / linked storno invoice

## Business Rules

- only `company-admin` can create storno / credit-note
- storno totals are negative or reversing, depending on chosen accounting representation
- payment implications must be explicitly defined:
  - fully unpaid original
  - partially paid original
  - fully paid original
- reports must account for storno documents correctly

## Desktop Scope

- invoice detail shows storno state and linked document references
- desktop exposes `Create Storno` / `Create Credit Note` only where allowed
- storno creation can start from original invoice detail
- resulting document should be viewable like any other invoice document

## Reporting Impact

- totals, paid amounts, and outstanding amounts must remain coherent
- reports summary must define whether values are:
  - gross before storno
  - or net after storno
- this choice must be explicit and consistent

## Technical Slice

- schema migration for invoice document type and original invoice link
- backend service and endpoint for storno creation
- integration tests for allowed and denied storno scenarios
- desktop invoice detail and action wiring

## Done When

- issued invoice can produce a storno / credit-note document
- original invoice stays immutable
- linked documents are visible in API and desktop
- tenant and role rules are enforced
- reports remain consistent after storno creation

# PLAN_28

## Goal

Add the first stable invoice PDF generation flow and a template foundation that works for both invoices and credit notes.

## Why

- the core financial workflow now exists end-to-end
- users still do not have the actual document output they need to send, print, archive, or review
- company branding is already present and should now appear on generated documents
- later template customization will be easier if we first establish one clean rendering pipeline

## Model Direction

- start with one default professional template
- support both `INVOICE` and `CREDIT_NOTE` in the same rendering system
- keep the first phase deterministic and backend-owned
- do not start with a full template designer

## PDF Strategy

- generate PDF on the backend, not inside the desktop renderer
- expose a document endpoint for download / preview
- render from invoice read data, not from ad-hoc desktop state
- ensure the output is stable regardless of client platform

## Backend Scope

- define PDF generation service for invoice documents
- add endpoint(s) such as:
  - `GET /invoices/{id}/pdf`
- support:
  - invoice header
  - company branding
  - client data
  - line items
  - totals
  - payment summary
  - credit note references when applicable
- ensure tenant-safe access to generated documents

## Template Scope

- introduce one default template with:
  - company name and logo
  - brand accent usage
  - invoice / credit note title
  - invoice number, issue date, due date
  - client block
  - line item table
  - subtotal, tax, total
  - payment / refund summary where relevant
- structure the rendering code so later we can add:
  - template variants
  - template customization
  - PDF branding settings

## Business Rules

- generated PDF must match persisted backend data exactly
- issued invoices and credit notes must always be printable
- draft invoices may be excluded in phase 1 unless we explicitly decide to support draft preview
- company branding must not break readability or document integrity

## Desktop Scope

- add `Preview PDF` / `Download PDF` actions in invoice detail
- support desktop open/save flow for generated PDF documents
- distinguish invoice vs credit note visually in the action labels or detail context

## Technical Slice

- backend rendering service and controller endpoint
- shared invoice-to-template mapping
- branded HTML/template layer or equivalent PDF pipeline
- integration tests for:
  - invoice PDF access
  - credit note PDF access
  - cross-tenant denial
  - missing document handling
- desktop wiring for preview/download

## Out Of Scope

- template editor
- multiple selectable templates
- drag-and-drop PDF designer
- email sending
- bulk export

## Done When

- issued invoices can be generated as PDF
- issued credit notes can be generated as PDF
- generated documents include company branding and correct financial data
- desktop can trigger PDF retrieval cleanly
- rendering foundation is ready for later template customization

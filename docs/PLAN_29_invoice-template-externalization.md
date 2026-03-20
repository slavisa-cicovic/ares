# PLAN_29 Invoice Template Externalization

## Goal
- move invoice PDF HTML template out of `InvoicePdfService`
- keep backend-owned PDF generation
- separate template layout from render/business logic
- prepare clean foundation for multiple templates later

## Scope
- move current inline HTML from [`InvoicePdfService.java`](/home/slavisa/devel/vs_code/ares/server/src/main/java/com/sc/ares/features/invoice/service/InvoicePdfService.java) into a resource template file
- introduce a small template loading/render layer in `server`
- keep current single-template behavior
- keep current PDF endpoint contract unchanged

## Resource Structure
- add a template file under `server/src/main/resources/templates/invoice/default.html`
- keep CSS and HTML together for now inside the same resource file
- do not introduce DB-backed templates in this step

## Rendering Model
- `InvoicePdfService` should stop building raw HTML with a giant `StringBuilder`
- service should instead:
  - load template text from resources
  - build a render context object / map
  - replace placeholders with escaped values
- placeholder strategy can stay simple in this step
  - no need for Thymeleaf or a heavy templating engine yet

## Data Preparation
- create a dedicated PDF view model, for example:
  - document metadata
  - issuer block
  - client block
  - line rows
  - totals
  - payment history
  - refund history
- keep formatting decisions in Java:
  - money formatting
  - quantity formatting
  - percent formatting
  - date formatting

## Behavior Rules
- `GET /invoices/{id}/pdf` remains unchanged
- issued invoice and credit note PDF generation remains unchanged
- existing branding support stays intact
- current English-first wording stays intact

## Out Of Scope
- DB-stored templates
- WYSIWYG template editor
- per-company custom HTML editing
- multi-template selection UI

## Verification
- existing invoice PDF integration tests must still pass
- generated PDF should remain visually equivalent to current version
- desktop `Preview PDF` and `Save PDF` flows should continue to work unchanged

## Expected Outcome
- `InvoicePdfService` becomes smaller and easier to maintain
- PDF template changes no longer require editing a large Java string block
- project is ready for future `templateKey` support without another major refactor

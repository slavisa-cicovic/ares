# PLAN_22 Desktop Item Module

## Goal

Implement the first real desktop `Items` workspace on top of the existing backend `item` module, using the same core UX pattern already established for `Clients`:

- list
- filters
- server paging
- create/edit dialog
- hook-form + zod validation
- role-aware actions

The result should be a usable desktop screen for browsing and maintaining tenant-scoped items.

## Scope

This plan covers:

- `Items` list screen in desktop navigation
- backend-connected item list loading
- search, status filter, sorting, and paging
- `New Item` and `Edit Item` dialog flows
- `company-admin` write actions
- read-only behavior for regular `user`
- item form schema and validation

This plan does not cover:

- invoice line item picker UX refinement
- bulk import/export
- item categories
- advanced pricing rules
- barcode support

## Backend Contract Assumption

The desktop module should consume the already existing backend `item` API.

Expected endpoints:

- `GET /items`
- `GET /items/{id}`
- `POST /items`
- `PUT /items/{id}`
- `DELETE /items/{id}`

Expected backend ownership and security rules remain unchanged:

- every item belongs to one company
- item reads are tenant-scoped
- `company-admin` can create/update/delete
- regular `user` can read but not write

## Desktop UX Pattern

The module should follow the same baseline interaction pattern as the desktop `Clients` module:

- top-level `Items` screen
- server-driven table list
- search and status filter in the toolbar
- create/edit opened in a dialog
- form implemented with `react-hook-form` wrappers
- validation implemented with `zod`

Target interaction:

- user opens `Items`
- sees paged tenant items
- can search and filter
- `company-admin` opens `New Item` or `Edit`
- form is submitted through the backend
- screen refreshes without losing overall workspace context

## Fields

The initial desktop item form should support:

- `name`
- `code`
- `unit`
- `price`
- `taxRate`
- `status`
- optional `description`

Initial validation direction:

- `name` is required
- `unit` is required
- `price` is required and must be non-negative
- `taxRate` is required and must be non-negative
- `code` is optional
- `description` is optional
- `status` should be represented in UI through a switch or another compact control if it keeps the form clearer than a select

## Desktop API Layer

The desktop API client should be extended with item-specific methods:

- `getItems`
- `createItem`
- `updateItem`
- optionally `deleteItem` if delete is included in the same delivery

Related item DTO and response types should be added under:

- `desktop/src/lib/api/types/item.ts`

Existing monolithic type file patterns must not be reintroduced.

## UI Structure

Expected desktop implementation slices:

- `desktop/src/features/items/ItemsScreen.tsx`
- `desktop/src/features/items/ItemFormDialog.tsx`
- `desktop/src/features/items/item-form.schema.ts`

The `Items` screen should be wired into app navigation and rendered through the main shell.

## Table Behavior

The item list should support:

- server paging
- page size changes
- search
- sort where backend supports it
- status filter

Search should be useful for item workflows. It should at minimum cover fields that users naturally expect:

- `name`
- `code`
- optionally `description` if backend already supports it or can be aligned cheaply later

## Role-Aware Behavior

`company-admin`:

- can create items
- can edit items
- can delete items if delete action is exposed in this phase

`user`:

- can browse items
- cannot see misleading write affordances
- should get a clean read-only screen state

## Visual Direction

The module should use the current desktop MUI foundation and theme system.

It should:

- reuse the new shell and surface language
- reuse the current table/dialog/form component layer
- stay visually consistent with `Clients`

It should not regress into ad hoc legacy CSS layout.

## Testing and Verification

Minimum verification for this plan:

- desktop build passes
- item list loads from backend
- create flow works for `company-admin`
- edit flow works for `company-admin`
- read-only behavior works for `user`
- paging and search are functional

Verification command:

```bash
cd /home/slavisa/devel/vs_code/ares/desktop
npm run build
```

## Acceptance Criteria

This plan is complete when:

- `Items` in desktop is no longer a placeholder
- real tenant items are loaded from backend
- `company-admin` can create and edit items through a dialog form
- `user` sees a usable read-only item workspace
- the screen follows the same quality bar as the current `Clients` module

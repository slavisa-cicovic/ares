# PLAN_21 Item Ownership

## Goal

Introduce ownership-aware authorization for the backend `item` module so that item write access reflects real bookkeeping workflows.

The target rule is:

- `company-admin` can create, update, and delete any item in the company
- regular `user` can read all company items
- regular `user` can create items
- regular `user` can update and delete only items that they created

This plan exists before the desktop `Items` module because the desktop UX must rely on correct backend ownership rules instead of simulating them client-side.

## Why

In practice, item maintenance is often performed by bookkeeping users rather than tenant administrators.

A pure role model such as:

- `company-admin` write
- `user` read-only

would be too restrictive for real Serbian bookkeeping workflows.

Ownership-based authorization is a better fit:

- bookkeeping users can maintain their own item records
- tenant administrators still retain full control
- regular users cannot modify items created by someone else

## Scope

This plan covers:

- item ownership persistence
- item authorization changes
- backend API behavior changes for item create/update/delete
- integration test coverage for item ownership rules

This plan does not cover:

- desktop `Items` screen implementation
- item categories
- audit history UI
- ownership transfer UI

## Data Model Changes

Each item should track who created it.

Required field:

- `created_by_user_id`

Optional future-friendly field:

- `updated_by_user_id`

For this plan, `created_by_user_id` is the key requirement.

The field should:

- reference `users.id`
- always belong to the same tenant as the item
- be assigned by backend code, never trusted from client input

## Ownership Rules

### Read

All authenticated tenant users who have item read access can view company items, regardless of who created them.

### Create

Both:

- `company-admin`
- `user`

can create items within their own company.

On create, backend sets:

- `company_id` from auth tenant context
- `created_by_user_id` from authenticated user

### Update

`company-admin`:

- can update any item in the company

`user`:

- can update only items where `created_by_user_id` equals the authenticated user id

### Delete

`company-admin`:

- can soft delete any item in the company

`user`:

- can soft delete only items where `created_by_user_id` equals the authenticated user id

## API Behavior

No endpoint shape changes are required.

Existing endpoints remain:

- `GET /items`
- `GET /items/{id}`
- `POST /items`
- `PUT /items/{id}`
- `DELETE /items/{id}`

Behavior changes:

- `POST /items` becomes allowed for regular `user`
- `PUT /items/{id}` and `DELETE /items/{id}` allow regular `user` only for owned items

## Authorization Direction

Authorization should not be based only on role checks in the controller layer.

Backend service logic should check:

- tenant scope
- role
- item ownership where relevant

Recommended service rule:

- resolve authenticated principal
- resolve target item by `id + company_id`
- if principal is `company-admin`, allow
- if principal is `user`, allow only if `item.createdByUser.id == principal.userId`
- otherwise deny

## Response Semantics

For cross-tenant requests:

- return `404`

For same-tenant but unauthorized ownership violations:

- return `403`

This keeps tenant isolation and ownership denial clearly separated.

## Testing

Integration tests should cover at minimum:

- `company-admin` can create item
- regular `user` can create item
- regular `user` can update own item
- regular `user` cannot update another user’s item in the same company
- regular `user` can delete own item
- regular `user` cannot delete another user’s item in the same company
- `company-admin` can update/delete any company item
- cross-tenant access still returns `404`

## Acceptance Criteria

This plan is complete when:

- items persist ownership through `created_by_user_id`
- regular users can create items
- regular users can edit/delete only their own items
- company admins retain full tenant-wide item control
- integration tests prove the ownership rules

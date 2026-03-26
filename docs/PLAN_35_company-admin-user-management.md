# PLAN_35 Company Admin User Management

## Goal

Enable `company-admin` users to manage users inside their own company workspace.

This plan intentionally stays within company-scoped user management and does not introduce email invites, self-registration, or first-login password rotation yet.

## Scope

### Backend

- Add company-scoped user listing for `company-admin`
- Add company-scoped user creation for `company-admin`
- Restrict visibility to users from the active company only
- Return a one-time temporary password after user creation
- Keep authorization explicit:
  - `company-admin` can list and create users in their own company
  - regular `user` cannot access this flow
  - `super-admin` keeps separate platform-wide admin endpoints from `PLAN_34`

### Desktop

- Add a `Users` screen to the tenant/company-admin navigation
- Show a paged table of users for the active company
- Add search and status filter
- Add create-user dialog
- Show the temporary password once after successful creation
- Make the screen read-only or hidden for regular `user`

## API Shape

### List company users

- Reuse or extend tenant-scoped user list API
- Filters:
  - `search`
  - `status`
  - `page`
  - `size`
  - `sort`

### Create company user

- Input:
  - `firstName`
  - `lastName`
  - `email`
  - `role`
- Role choices for initial version:
  - `user`
  - optionally `company-admin` if we decide admins may create peer admins

### Response

- Created user summary
- Temporary password shown once

## UX Notes

- Tenant navigation should expose `Users` only when the authenticated user is `company-admin`
- Regular `user` should not see `Users` in the navigation
- The create dialog should match the super-admin create-user flow where possible
- After create:
  - refresh the table
  - open a confirmation dialog with the temporary password

## Out of Scope

- Email invite flow
- Password reset flow
- Forced password change on first login
- User edit/deactivate/delete
- User avatar/profile management beyond existing self-profile settings

## Verification

- Backend integration tests:
  - `company-admin` can list only own-company users
  - `company-admin` can create user for own company
  - regular `user` is rejected
  - cross-company access is rejected
- Desktop verification:
  - `company-admin` sees `Users` nav item and screen
  - regular `user` does not
  - create flow returns temporary password and refreshes list

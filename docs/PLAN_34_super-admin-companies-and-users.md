# PLAN_34

## Title
Super Admin Companies And Users

## Goal
Introduce the first real platform-admin workflow for a deployed Ares instance so a `super-admin` can:
- sign in
- see all companies
- create a new company with minimal business data
- see all users across companies
- create the first `company-admin` for a selected company

This plan intentionally does **not** include email invites. New users will be created with a temporary password and a future first-login password rotation flow.

## Scope

### In scope
- backend `super-admin` company list/create capabilities
- backend `super-admin` user list/create capabilities
- desktop navigation and screens for `super-admin`
- company creation with minimal required data:
  - `name`
  - `pib`
  - `mb`
- user creation for a selected company with:
  - `firstName`
  - `lastName`
  - `email`
  - `role`
- temporary password generation/display for newly created users
- role-based access so only `super-admin` can use these platform-wide flows

### Out of scope
- email invitation flow
- forgot-password flow
- forced password change on first login
- full company branding/settings editing from the `super-admin` console
- company-admin self-service user management for their own company
- user deactivation/reactivation/reset-password polish

## Product Decisions

### Admin model
- `super-admin` is a platform-level actor, not tied to one operational company workspace in the same way as `company-admin` and `user`
- `super-admin` gets dedicated desktop views for:
  - `Companies`
  - `Users`
- these views must show platform-wide data, not just active-company data

### Company creation
- the initial create-company flow is intentionally minimal
- required fields:
  - `name`
  - `pib`
  - `mb`
- all other company details can be completed later by the tenant through company settings

### User onboarding
- user creation is desktop-first and does not depend on email delivery
- when a `super-admin` creates a user, the backend generates a temporary password
- the desktop UI shows the temporary password once, immediately after create
- the `super-admin` is responsible for delivering that credential out-of-band

## Backend Work

### Companies
- add `super-admin` list endpoint for all companies
- add `super-admin` create-company endpoint
- reuse existing uniqueness rules for `pib` and `mb`
- return compact company summaries suitable for platform admin tables

### Users
- add `super-admin` list endpoint for all users across companies
- include company reference in user summary response
- add `super-admin` create-user endpoint with:
  - `companyId`
  - `firstName`
  - `lastName`
  - `email`
  - `role`
- backend generates a temporary password and returns it in the create response

### Authorization
- only `super-admin` can call these new platform-wide endpoints
- existing tenant-scoped endpoints must remain unchanged

## Desktop Work

### Navigation
- adapt shell/navigation for `super-admin`
- `super-admin` should see platform-admin entries:
  - `Companies`
  - `Users`
- existing tenant-oriented modules should be hidden or intentionally limited if they do not make sense for `super-admin`

### Companies screen
- implement `SuperAdminCompaniesScreen`
- features:
  - platform-wide list
  - search
  - create company dialog
- create dialog fields:
  - `name`
  - `pib`
  - `mb`

### Users screen
- implement `SuperAdminUsersScreen`
- features:
  - platform-wide list
  - search
  - company column
  - role column
  - create user dialog
- create-user dialog fields:
  - company selector
  - first name
  - last name
  - email
  - role

### Temporary password UX
- after successful user creation, show a success dialog or inline result block containing:
  - created user identity
  - temporary password
- clearly state that the password is shown once and should be delivered securely

## Validation

### Company form
- `name` required
- `pib` required
- `mb` required

### User form
- `companyId` required
- `firstName` required
- `lastName` required
- `email` required and valid
- `role` required

## Verification
- backend integration tests for:
  - `super-admin` can list companies
  - `super-admin` can create company
  - `super-admin` can list users
  - `super-admin` can create company-admin for a company
  - non-`super-admin` users are forbidden
- desktop build must pass
- manual smoke flow:
  - sign in as `super-admin`
  - create company
  - create `company-admin`
  - verify temporary password is shown

## Follow-up Plans
- `PLAN_35_company-user-management.md`
- `PLAN_36_first-login-password-rotation.md`

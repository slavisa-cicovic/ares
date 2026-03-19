# PLAN_03A_dev-seed-data.md

## Project
Ares

## Goal
Provide a controlled development-only seed dataset so local backend work can be tested against realistic multi-tenant users, companies, and role assignments from the start.

---

## Summary

This plan introduces development seed data for Ares.
The purpose is to make local development and manual testing easier by giving the backend a predictable set of users and companies that exercise tenant isolation and role behavior.

The seed should support:

- at least two companies
- one platform `super-admin`
- one `company-admin` per company
- one regular `user` per company

This seed is for development only.
It must not become a production bootstrap mechanism and must not be required for tests unless explicitly requested in a separate test fixture strategy.

---

## Scope

This plan covers:

- development-only seed execution
- dev companies and users
- built-in role assignment for seeded users
- password hashing for seeded accounts
- idempotent seed behavior
- local developer documentation for seeded credentials

This plan does not cover:

- production bootstrap users
- test fixture strategy for integration tests
- email verification flows
- password reset flows
- seeding business modules such as clients, items, invoices, or payments

---

## Why This Exists

Development seed data is useful because it makes tenant and role behavior visible early.

Benefits:

- validates that multiple companies can coexist locally
- makes cross-tenant data leakage easier to detect
- allows quick manual testing for `super-admin`, `company-admin`, and `user`
- helps desktop and mobile clients later by giving them stable local identities to log in with

Without seed data, local auth and tenant testing becomes slow and error-prone.

---

## Seed Dataset

The initial development dataset should include:

- 2 companies
- 1 platform `super-admin`
- 1 `company-admin` for company A
- 1 regular `user` for company A
- 1 `company-admin` for company B
- 1 regular `user` for company B

Total users:

- 5 users

This is enough to validate:

- platform administration behavior
- tenant-scoped administration
- tenant-scoped regular user behavior
- cross-tenant isolation

---

## Development Company Direction

Use clear development-only company names.

Example direction:

- `Ares Demo One`
- `Ares Demo Two`

Requirements:

- names should make it obvious these are local dev tenants
- company records should use normal active lifecycle values
- no physical delete behavior should be involved

---

## Development User Direction

Use obvious local-only identities.

Suggested direction:

- one platform admin account
- two company admin accounts
- two regular user accounts

Rules:

- email addresses should clearly indicate local development use
- passwords must be stored as password hashes, not plain text
- user status should be active
- all users should be non-deleted records

Suggested naming convention:

- `superadmin@local.ares`
- `admin.one@local.ares`
- `user.one@local.ares`
- `admin.two@local.ares`
- `user.two@local.ares`

The exact addresses may be adjusted during implementation, but they should stay stable and easy to remember.

---

## Role Assignment Direction

Seeded role assignment should reflect the current built-in role model:

- the platform admin gets `super-admin`
- each company admin gets `company-admin`
- each regular user gets `user`

Rules:

- tenant users must belong to their own company only
- the `super-admin` must be clearly separated from normal tenant administration use
- role assignment must use the same persistent role model as the real application

This is important because the development seed should validate the real authorization structure, not a fake shortcut model.

---

## Execution Strategy

The seed must run only in development-oriented runtime conditions.

Recommended approach:

- use a Spring `ApplicationRunner` or `CommandLineRunner`
- activate it only for the `dev` profile
- execute after migrations and after the built-in role records exist

Rules:

- dev seed logic must not run in production
- dev seed logic must not run in `test` unless explicitly enabled
- seed logic must reuse repositories and password encoder, not raw ad hoc inserts

---

## Idempotency Rules

The seed must be safe to run repeatedly.

Required behavior:

- if seed companies already exist, do not create duplicates
- if seed users already exist, do not create duplicates
- if expected role assignments already exist, do not duplicate them
- rerunning the app in `dev` should converge to one stable dataset

Recommended strategy:

- identify seeded companies and users by stable unique keys such as company name or email
- create missing records only
- update only carefully chosen fields when needed

Avoid:

- destructive reseeding
- wiping local data automatically
- assuming an empty database

---

## Relationship To Flyway

Development users and companies should not be seeded through Flyway migrations.

Recommended split:

- Flyway handles schema and platform-defined structural data such as built-in roles and permissions
- dev seed bootstrap handles development companies and users

Reason:

- dev accounts are environment-specific
- Flyway should stay focused on stable schema and structural reference data
- development seed data should remain optional and profile-bound

---

## Password Handling

Seeded users must use the real password hashing mechanism.

Rules:

- use the application password encoder
- never commit plain-text password hashes that were copied from ad hoc tooling without context
- if the seed defines known local passwords, document them in developer-facing docs only as development credentials

Recommended direction:

- keep the actual seed code readable
- keep passwords simple for development, but clearly local-only

---

## Documentation

The seeded accounts should be documented for developers.

Documentation should include:

- company names
- email addresses
- role of each account
- local-only password values
- a reminder that these credentials are for development only

Likely locations:

- `README.md`
- or a dedicated server development note if the list grows later

---

## Deliverables

- development-only seed mechanism exists in `server`
- two companies are created in `dev`
- one `super-admin` user exists
- one `company-admin` and one `user` exist for each demo company
- passwords are hashed through the real encoder
- seed execution is idempotent
- development credentials are documented

---

## Acceptance Criteria

- starting the backend in `dev` on an empty local database creates the expected companies and users
- restarting the backend in `dev` does not create duplicates
- seeded tenant users belong to the correct company
- seeded users receive the correct built-in roles
- the seed does not run in `test`
- the seed is clearly local-development-only behavior

---

## Risks

- putting dev user seed logic into Flyway can pollute non-dev environments
- non-idempotent seed logic can create duplicate users and broken role assignments
- using fake shortcuts that bypass real password or role logic reduces the value of the seed
- failing to document seed credentials makes local setup harder than it needs to be

---

## Next Step After This Plan

After this plan, the next practical step is to implement:

- a `dev`-profile seed runner
- seeded companies
- seeded users
- role assignments through the existing auth and tenant model

This will make later auth and client integration work much easier to test manually.

---

## Assumptions

- built-in roles already exist as structural seed data
- the application password encoder is available
- `dev` remains the default local profile
- the local PostgreSQL database is available through the existing Docker setup

---

## Notes

- Seed data should validate the real architecture, not bypass it.
- Keep the dataset small, stable, and intentionally multi-tenant.

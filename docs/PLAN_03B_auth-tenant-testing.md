# PLAN_03B_auth-tenant-testing.md

## Project
Ares

## Goal
Define how Ares should test authentication, tenant isolation, and role-based authorization so security-critical behavior is verified systematically as backend features grow.

---

## Summary

This plan establishes the testing strategy for auth and tenant behavior in the Ares backend.
It exists to make security expectations executable rather than implicit.

The objective is not only to test successful access paths, but to make sure the backend reliably rejects:

- unauthenticated access
- unauthorized access
- cross-tenant access
- role misuse
- invalid user states such as inactive or deleted accounts

This plan should guide both immediate tests for the current auth foundation and future tests once real login and token flows are implemented.

---

## Scope

This plan covers:

- auth-related test categories
- tenant isolation test categories
- role and permission test matrix direction
- integration test approach
- repository and service test expectations
- negative test expectations
- relationship between seed data and tests

This plan does not cover:

- full UI testing for desktop or mobile
- external identity provider testing
- performance or load testing
- penetration testing methodology
- final CI pipeline design

---

## Why This Matters

Auth and tenant bugs are high-risk defects.
If they are not tested explicitly, they often survive ordinary feature testing.

This is especially important in Ares because:

- the system is multi-tenant
- users belong to companies
- data must not cross tenant boundaries
- authorization will combine platform roles, tenant roles, and later permissions

Testing must prove the absence of cross-tenant leakage, not merely assume it.

---

## Testing Principles

The auth and tenant test strategy should follow these principles:

- test denial paths as seriously as success paths
- prefer integration tests for security boundaries
- validate tenant isolation at more than one layer
- avoid relying only on happy-path controller tests
- keep role expectations explicit
- keep the test dataset small and understandable

The most important failures to catch are usually negative cases.

---

## Test Layers

### 1. Application Context Tests

These tests verify that the auth and tenant foundation boots correctly.

Coverage should include:

- Spring context startup
- security configuration wiring
- repository scanning
- tenant-related shared services

This layer already exists in minimal form and should continue to serve as a fast baseline.

### 2. Integration Tests

This should be the primary layer for auth and tenant behavior.

Coverage should include:

- endpoint access control
- unauthorized vs forbidden behavior
- role-based access
- tenant-scoped resource access
- real database-backed authorization behavior where practical

Recommended tools:

- Spring Boot integration tests
- MockMvc or equivalent HTTP-level testing
- test database setup aligned with the application model

### 3. Service-Level Tests

Service tests should verify tenant-aware business behavior where authorization or scoping is applied below the controller layer.

Coverage should include:

- tenant context usage
- service-level authorization guards
- company-scoped query behavior
- role/permission-dependent service rules

### 4. Repository Tests

Repository tests should verify persistence assumptions that matter for tenant safety.

Coverage should include:

- tenant-scoped query patterns
- exclusion of soft-deleted data where required
- uniqueness rules relevant to tenant and user identity

Repository tests are especially important if query complexity grows later.

---

## Core Auth Test Categories

### 1. Anonymous Access

Required scenarios:

- anonymous request to protected route returns `401`
- anonymous request to public route succeeds when intended

This verifies that the backend is secure by default.

### 2. Authenticated but Unauthorized Access

Required scenarios:

- authenticated user without required authority gets `403`
- authenticated user cannot access admin-only routes without admin rights

This distinguishes missing authentication from missing authorization.

### 3. User State Restrictions

Once real authentication is implemented, test:

- inactive user cannot authenticate
- suspended user cannot authenticate or cannot operate, according to final rule
- soft-deleted user cannot authenticate

These tests should be added as soon as the login flow exists.

---

## Core Tenant Test Categories

### 1. Same-Tenant Access

Required scenarios:

- a company user can access records belonging to their own company when authorized
- company-scoped admin actions work only within the same tenant

### 2. Cross-Tenant Denial

Required scenarios:

- a user from company A cannot read company B data
- a user from company A cannot modify company B data
- a company admin from company A cannot administer users in company B

This is one of the highest-priority test areas in the system.

### 3. Platform Exception Paths

Required scenarios:

- `super-admin` behavior is explicit and tested
- any cross-tenant capability allowed to `super-admin` must be clearly intentional
- no ordinary tenant role may behave like `super-admin`

The goal is to make platform override behavior explicit and reviewable.

---

## Role Test Matrix Direction

The following matrix should become part of integration testing as auth grows:

- anonymous
- `user`
- `company-admin`
- `super-admin`

For each protected capability, define expected behavior for:

- allowed
- forbidden
- tenant-scoped allowed
- cross-tenant forbidden

This matrix should be kept small at first and expanded with real modules.

---

## Permission Testing Direction

Current auth foundation is role-oriented, but the model is moving toward permission-based RBAC.

Testing direction should therefore prepare for:

- role-to-permission mapping verification
- built-in role permission coverage
- future custom role permission coverage
- denial when permission is missing even if authentication is valid

When permission enforcement becomes real, tests should target permissions directly, not only role names.

---

## Seed Data and Tests

Development seed data from `PLAN_03A` is useful for manual testing, but automated tests should not depend on dev seed bootstrap by default.

Rules:

- automated tests should control their own data setup
- `dev` seed logic should not be required for `test`
- test fixtures should create only the minimum data needed per scenario

Manual verification can use the dev seed accounts.
Automated verification should remain isolated and deterministic.

---

## Test Data Strategy

Recommended automated test data patterns:

- company A and company B
- one or more users per company
- explicit role assignments
- explicit active/inactive/deleted user states where relevant

Rules:

- test data should make tenant boundaries obvious
- use stable naming in tests
- avoid giant shared fixtures when a small scenario-specific setup is enough

---

## Initial Test Scenarios

The first practical scenarios to implement after this plan should include:

1. public health endpoint is reachable without authentication
2. a protected endpoint returns `401` when unauthenticated
3. a protected endpoint returns `403` when authenticated without authority
4. same-tenant access succeeds when authorized
5. cross-tenant access fails even when the caller is otherwise valid
6. `super-admin` exception behavior is explicit

These first tests should shape the pattern for later module-specific security tests.

---

## Deliverables

- documented auth and tenant testing strategy
- defined test layers for auth and tenant concerns
- defined role and tenant scenario matrix
- defined negative test expectations
- clear separation between dev seed data and automated test fixtures

---

## Acceptance Criteria

- the plan explicitly requires `401` testing
- the plan explicitly requires `403` testing
- the plan explicitly requires cross-tenant denial testing
- the plan explicitly requires explicit `super-admin` testing
- the plan explicitly states that automated tests should not rely on dev seed bootstrap

---

## Risks

- relying only on happy-path feature tests can hide serious tenant leaks
- testing only controllers can miss service or repository scoping bugs
- overusing shared fixtures can make auth tests hard to understand
- using dev seed data as automated test truth can make tests brittle

---

## Next Step After This Plan

After this plan, the next practical work can include:

- adding the first protected test endpoint or auth test fixture
- creating integration tests for `401` and `403`
- creating initial tenant-isolation integration tests

This should happen before a large number of business modules are added.

---

## Assumptions

- auth and tenant behavior remain backend responsibilities
- `super-admin`, `company-admin`, and `user` remain the initial built-in roles
- dev seed data remains development-only
- future auth implementation will introduce real token or session behavior that can extend these tests

---

## Notes

- Security behavior should be proven with tests, not inferred from code structure.
- Cross-tenant denial cases are first-class acceptance criteria for the backend.

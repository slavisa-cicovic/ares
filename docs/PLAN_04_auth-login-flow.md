# PLAN_04_auth-login-flow.md

## Project
Ares

## Goal
Implement the first real authentication use case in the backend so users can log in with email and password, the backend can validate account and tenant state, and clients can start integrating against a real auth contract.

---

## Summary

This plan is the first concrete auth implementation phase after the foundation work.
The objective is to replace placeholder authentication with a real login flow backed by the database model introduced in earlier plans.

This phase should produce:

- a login endpoint
- credential validation against persisted users
- password verification through `PasswordEncoder`
- rejection of inactive, suspended, or deleted users
- an authenticated response model that includes identity and tenant context

This phase does not need to finish the entire long-term token story, but it must create a real backend-authenticated login contract.

---

## Scope

This plan covers:

- login request and response DTOs
- email/password authentication
- user lookup from the database
- account state checks
- company state checks
- initial authenticated response payload
- current-user identity endpoint direction
- integration tests for login behavior

This plan does not cover:

- full refresh token lifecycle
- password reset
- email verification
- MFA
- SSO or external providers
- full permission enforcement by token claims

---

## Why This Comes Next

The backend already has:

- tenant and role model direction
- auth and tenant foundation
- development seed users
- baseline auth and tenant tests

What is missing is a real login flow.
Without that, desktop and mobile cannot integrate against an actual auth contract, and user status rules are not yet enforced end-to-end.

---

## Core Decisions

This phase should lock the following:

- login identifier is email
- credentials are validated in the backend
- password verification uses the configured `PasswordEncoder`
- deleted users cannot log in
- inactive users cannot log in
- suspended users cannot log in
- users belonging to deleted or inactive/suspended companies must not be treated as valid normal logins

The backend remains the source of truth for:

- who the user is
- which company the user belongs to
- which roles the user has

---

## Login Request

The first login request should stay simple.

Suggested shape:

- `email`
- `password`

Rules:

- email matching should be case-insensitive
- validation should reject blank input
- the request must not contain role or company claims as trusted auth input

---

## Login Response

The initial login response should be useful to clients even before the final token strategy is complete.

Suggested response direction:

- user id
- email
- first name
- last name
- company id
- company name
- assigned built-in roles
- token or session material for the chosen phase approach

If token issuance is not fully implemented in this phase, the contract should still prepare for it cleanly.

---

## Authentication Service Flow

Recommended flow:

1. validate request payload
2. find user by email, excluding soft-deleted users
3. reject if user does not exist
4. reject if password does not match
5. reject if user status is not loggable
6. reject if the owning company is not loggable
7. load the user's roles
8. build authenticated response

Rules:

- do not leak whether the email or password was wrong in overly specific error messages
- keep failure responses safe and consistent
- keep the login orchestration in a service, not in the controller

---

## User State Rules

This phase should define explicit login behavior for user state.

Required rules:

- `ACTIVE` users may log in
- `INACTIVE` users may not log in
- `SUSPENDED` users may not log in
- users with `deleted_at` set may not log in

This must be enforced in backend code, not assumed by clients.

---

## Company State Rules

The company tenant state also affects login.

Required direction:

- users of a deleted company must not log in as normal tenant users
- users of an inactive or suspended company must not be treated as valid standard logins unless an explicit future exception is introduced

This prevents users from entering an unusable tenant context.

---

## Role Loading

Login must return or derive the user's roles from the persisted role model.

Requirements:

- load assigned roles from `user_roles`
- distinguish platform and company roles where needed
- prepare the principal model to carry usable auth context into later requests

Even if later token claims change, the login service should already use the real role assignments.

---

## Token / Session Direction

The long-term auth direction remains stateless token-based auth.

For this phase, choose one of these approaches explicitly:

### Preferred direction

- issue access token and refresh token from the first real login implementation

Why:

- this aligns immediately with desktop and mobile API usage
- avoids temporary auth models that will be thrown away

### Acceptable temporary direction

- implement login response and current-user contract first
- add actual signed token issuance in the next auth phase

If this temporary direction is chosen, it must be short-lived and deliberate.

---

## Current User Endpoint

This phase should prepare for an authenticated identity endpoint.

Suggested direction:

- `GET /auth/me`

Purpose:

- return the authenticated user's identity
- return company context
- return roles visible to the client

This endpoint is highly useful for desktop and mobile bootstrapping after login.

---

## Package Direction

Implement this work primarily under:

- `features/auth/controller`
- `features/auth/dto`
- `features/auth/service`
- supporting repository changes where necessary

Keep rules:

- controller stays thin
- service handles auth orchestration
- repository layer provides needed user/role loading

---

## Error Handling Direction

Login failure handling must be explicit and safe.

Recommended behavior:

- invalid credentials return a consistent authentication failure response
- inactive, suspended, deleted, or otherwise unusable accounts return a safe auth failure response
- validation errors return standard validation responses

Do not expose unnecessary internal account-state detail to the client unless there is a deliberate product reason.

---

## Testing Expectations

This phase should add integration tests for:

- successful login with valid seeded or test-created user
- invalid password
- unknown email
- inactive user
- suspended user
- deleted user
- user whose company is inactive or deleted
- role loading in successful auth result

These tests extend `PLAN_03B`.

---

## Relationship To Seed Data

Development seed users from `PLAN_03A` may help manual verification.

Rules:

- automated tests should still create controlled test data
- dev seed data should not become the primary automated auth test fixture

Manual checks may use:

- `superadmin@local.ares`
- `admin.one@local.ares`
- `user.one@local.ares`
- `admin.two@local.ares`
- `user.two@local.ares`

---

## Deliverables

- login endpoint exists
- login request DTO exists
- login response DTO exists
- backend validates password against stored hash
- backend rejects invalid user and company states
- user roles are loaded from persistent assignment
- auth integration tests cover success and denial paths

---

## Acceptance Criteria

- a valid active user can log in
- invalid credentials are rejected
- inactive, suspended, and soft-deleted users cannot log in
- users of invalid company state cannot log in
- login response carries enough identity and tenant context for clients
- the implementation aligns with the future token-based direction

---

## Risks

- overexposing auth failure reasons can leak account information
- shortcutting role loading at login can create divergence from the real auth model
- introducing a temporary auth contract that clients depend on can create later rework
- not testing invalid account states can leave serious auth gaps

---

## Next Step After This Plan

After this plan, the likely next steps are:

- implement token refresh and logout behavior
- implement `GET /auth/me`
- start desktop auth integration against real backend login

---

## Assumptions

- email remains the login identifier
- password hashes are already part of the user model
- the backend continues toward token-based auth for clients
- one user still belongs to one company in v1

---

## Notes

- This is the first auth flow that clients will depend on, so the contract should stay clean and intentional.
- Login should validate real account state, not just credentials.

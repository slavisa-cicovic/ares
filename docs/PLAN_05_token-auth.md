# PLAN_05_token-auth.md

## Project
Ares

## Goal
Introduce the first real client-facing token authentication model so desktop and mobile clients can authenticate without relying on HTTP Basic, while preserving the multi-tenant and role rules already defined in earlier plans.

---

## Summary

This plan moves the backend from temporary Basic-auth-backed development authentication toward the intended token-based model.

This phase should produce:

- access token issuance on login
- refresh token issuance and refresh flow
- logout and token revocation direction
- bearer-token authentication for protected endpoints
- continued support for tenant-aware identity and role context

This phase should replace HTTP Basic as the primary client auth mechanism.

---

## Scope

This plan covers:

- token model direction
- login response update to include token material
- refresh endpoint
- logout endpoint
- bearer token authentication filter direction
- server-side refresh token persistence and revocation strategy
- integration tests for token flows

This plan does not cover:

- external identity providers
- MFA
- password reset
- email verification
- audit log finalization
- fine-grained permission claims optimization

---

## Why This Comes Next

The backend already has:

- real login with email and password
- `GET /auth/me`
- tenant-aware principal model
- integration tests for auth and tenant behavior

What is still missing is the actual auth contract that desktop and mobile will use in normal operation.
HTTP Basic was acceptable as a bridge, but it should not remain the real client-facing mechanism.

---

## Core Decisions

This phase should lock the following:

- access to protected APIs should use bearer tokens
- login should issue an access token and a refresh token
- refresh tokens must be persisted server-side
- refresh tokens must be revocable
- logout must invalidate the current refresh token
- backend remains the source of truth for tenant and role context

The token model must not weaken tenant isolation.

---

## Token Model Direction

Recommended direction:

- short-lived access token
- longer-lived refresh token

Suggested baseline:

- access token: minutes, not days
- refresh token: days, not months, for the first implementation

Access token purpose:

- authenticate API requests
- carry stable user identity and tenant context

Refresh token purpose:

- obtain a new access token
- support long-lived desktop and mobile sessions without re-entering credentials too frequently

---

## Access Token Contents

Access token claims should stay minimal and intentional.

Suggested direction:

- user id
- company id
- email
- platform-admin flag
- role codes or an equivalent authority representation
- issued-at and expiration
- token id if useful for tracing

Rules:

- do not trust client-provided tenant information
- do not include mutable business data that will go stale quickly
- permission catalogs should remain backend-defined even if roles are present in token claims

---

## Refresh Token Persistence

Refresh tokens should not be treated as purely stateless in v1.

Recommended direction:

- store refresh tokens in the database
- persist token id, user id, expiration, revoked-at, created-at
- store only hashed refresh token material if practical in the first implementation

Why:

- logout and revocation become straightforward
- compromised refresh tokens can be invalidated
- future device/session management stays possible

---

## Login Response Update

`POST /auth/login` should evolve from identity-only response to token-bearing response.

Suggested response direction:

- access token
- refresh token
- access token expiration
- user identity payload
- company context
- role list

The response should remain easy for clients to consume and should not require additional bootstrap calls just to identify the logged-in user.

---

## Refresh Endpoint

Suggested direction:

- `POST /auth/refresh`

Behavior:

1. receive refresh token
2. validate signature or token format
3. verify persisted refresh token record
4. reject revoked, expired, or unknown token
5. reject if user or company is no longer valid for login
6. issue new access token
7. optionally rotate refresh token

Recommended v1 approach:

- refresh token rotation is preferred
- if not implemented immediately, the plan must state that refresh token reuse detection is deferred explicitly

---

## Logout Endpoint

Suggested direction:

- `POST /auth/logout`

Behavior:

- revoke the presented refresh token
- return success even if the token is already effectively unusable, when safe to do so

This endpoint should be simple and idempotent.

---

## User And Company State Revalidation

Token auth must not bypass account-state rules.

Required direction:

- login validates user and company state
- refresh validates user and company state again
- protected request handling may trust a valid access token for the request itself
- newly inactive, suspended, or deleted users must be prevented from receiving fresh access tokens through refresh

This ensures auth state remains consistent with the domain model.

---

## Security Filter Direction

Protected API routes should authenticate through a bearer-token filter or equivalent Spring Security mechanism.

Requirements:

- parse bearer token from `Authorization` header
- validate token
- reconstruct authenticated principal
- populate Spring Security context
- preserve compatibility with tenant context services

HTTP Basic can remain temporarily available for local development if there is a clear reason, but bearer token auth should become the intended standard.

---

## Persistence Direction

This phase likely needs a new auth persistence table, for example:

- `refresh_tokens`

Suggested fields:

- `id`
- `user_id`
- `token_hash` or equivalent persisted token identifier
- `expires_at`
- `revoked_at`
- `created_at`
- optional `replaced_by_token_id`
- optional device or client label for future session management

No physical delete should be required for normal revocation handling.

---

## Package Direction

Implement this work primarily under:

- `features/auth/controller`
- `features/auth/dto`
- `features/auth/entity`
- `features/auth/repository`
- `features/auth/service`
- `common/security`

Keep rules:

- controller stays thin
- token issuance and validation belong in services
- persistence of refresh tokens remains explicit

---

## Error Handling Direction

Token failure handling must remain safe and consistent.

Recommended behavior:

- invalid login credentials return safe `401`
- invalid access token returns safe `401`
- expired access token returns safe `401`
- invalid refresh token returns safe `401`
- revoked refresh token returns safe `401`

Avoid leaking internal details such as whether a token existed in persistence or whether a specific user account is present.

---

## Testing Expectations

This phase should add integration tests for:

- successful login returning tokens
- authenticated request with valid access token
- request with missing bearer token
- request with invalid bearer token
- refresh with valid refresh token
- refresh with revoked refresh token
- refresh with expired refresh token
- logout revoking refresh token
- refresh rejection when user becomes inactive
- refresh rejection when company becomes inactive

These tests should extend the auth testing foundation from `PLAN_03B` and the login tests from `PLAN_04`.

---

## Relationship To Clients

This phase is the first auth contract that `desktop` and `mobile` should realistically target.

Desktop expectations:

- store token material securely
- attach bearer token to API requests
- use refresh flow on access token expiry

Mobile expectations:

- same API contract as desktop
- secure storage strategy decided later in the mobile implementation phase

The backend contract should stay neutral enough that both clients can use it without separate auth models.

---

## Deliverables

- token auth plan is explicit
- login returns token material
- refresh endpoint exists
- logout endpoint exists
- bearer-token auth works for protected endpoints
- refresh token persistence exists
- integration tests cover happy and denial paths

---

## Acceptance Criteria

- clients can authenticate without HTTP Basic
- a valid access token authenticates protected requests
- invalid or expired tokens are rejected
- revoked refresh tokens cannot be reused
- inactive, suspended, or deleted accounts cannot continue refreshing sessions
- tenant identity remains backend-controlled

---

## Risks

- embedding too much mutable data in tokens can create stale auth context
- making refresh tokens fully stateless weakens revocation control
- skipping refresh-token persistence creates future rework
- inconsistent revalidation of user or company state can create security gaps

---

## Next Step After This Plan

After this plan, the likely next steps are:

- implement token auth
- integrate desktop against token login and `/auth/me`
- define first protected business module contract

---

## Assumptions

- email remains the login identifier
- one user still belongs to one company in v1
- token auth is the intended production direction for both desktop and mobile
- backend remains the source of truth for roles and tenant context

---

## Notes

- This phase should end the period where HTTP Basic is the practical auth mechanism for clients.
- Refresh token persistence is worth doing correctly now instead of retrofitting it later.

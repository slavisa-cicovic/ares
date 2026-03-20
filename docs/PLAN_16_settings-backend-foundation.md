# PLAN_16_settings-backend-foundation.md

## Project
Ares

## Goal
Extend the backend so the new desktop settings shell can move from placeholder structure to real company and profile management capabilities.

---

## Summary

The desktop app now has a real `Settings` area with:

- company settings
- profile settings
- appearance settings

That shell exposed the next backend gap clearly:

- company settings currently support only `name`
- user profile settings do not yet support password change
- logo and avatar do not yet have a storage/reference strategy

This phase should close the most important backend gaps needed for the next real settings slice.

---

## Scope

This plan covers:

- company profile model expansion
- company settings API expansion
- password change for the authenticated user
- profile/settings direction for future avatar and logo support

This plan does not cover:

- binary asset upload implementation
- image storage infrastructure
- invoice template editing
- full theme preference persistence unless it stays small and clean

---

## Why This Comes Next

The desktop shell now has a natural place for settings, but the backend still exposes only a minimal company model.

Without this step:

- desktop company settings cannot become real
- password change cannot move beyond placeholder state
- future branding work will remain structurally blocked

This phase should make the settings area useful, not just well-organized.

---

## Core Decisions

This phase should lock the following:

- company profile data is tenant-owned and edited by `company-admin`
- password change is user-owned and applies only to the authenticated user
- asset upload remains out of scope for now unless implemented as a simple reference field
- backend should expose enough company profile data for desktop forms to become real

Recommended practical direction:

- implement real company profile fields now
- implement password change now
- postpone actual logo/avatar upload

---

## Company Model Direction

The company model should be extended to support:

- `name`
- `pib`
- `mb`
- `address`
- `email`
- `phone`
- `bankAccount`

Optional placeholder field if it stays clean:

- `logoAssetKey` or equivalent reference-only field

This phase should define:

- nullable vs required fields for v1
- validation rules and length limits
- whether uniqueness is required for fields such as `pib` or `mb`

Recommended v1 direction:

- `name` remains required
- the rest may start optional
- validation should still be explicit where fields are provided

---

## Company API Direction

This phase should expand:

- `GET /company`
- `PUT /company`

The company response should include the expanded company settings fields.

The update request should support editing the same fields.

Rules:

- only `company-admin` can update company settings
- tenant scope remains derived from authenticated context
- no client-provided tenant override is allowed

Desktop should then be able to use the same screen structure with real backend data.

---

## Password Change Direction

The backend should add a dedicated authenticated-user password change endpoint.

Recommended endpoint:

- `POST /users/me/change-password`

Recommended request fields:

- `currentPassword`
- `newPassword`
- `confirmNewPassword`

Rules:

- user may change only their own password
- current password must be verified
- new password must pass minimum validation
- password must be stored through the existing password encoder
- invalid current password should not return ambiguous success

Recommended behavior:

- return `204 No Content` on success
- return `400` for invalid request shape
- return `401` or `400` with clear message for wrong current password depending on current API style

---

## Avatar And Logo Direction

This phase should not force full upload infrastructure.

However, it should explicitly choose one of the following directions:

1. no avatar/logo backend fields yet
2. add reference-only fields now, but not upload endpoints yet

My recommended direction for this phase:

- keep upload out of scope
- optionally add reference fields only if they do not distort the domain model

Reason:

- file handling is a separate concern
- it should not dilute the current settings phase

---

## Theme Preference Direction

Desktop currently supports local theme selection.

This phase should decide whether backend persistence is needed now.

Recommended direction:

- keep theme preference local for now
- revisit backend persistence after company-wide vs user-level theme ownership is decided

Reason:

- this is not yet a business-critical setting
- ownership is still conceptually open

---

## Validation And Security

This phase must keep strong validation around:

- company field lengths and formats
- password change rules
- role-based access for company settings

Password handling rules:

- never return password hash or sensitive auth details
- never allow password change without current password verification
- soft-deleted or inactive users should follow existing auth rules consistently

---

## Persistence Direction

This phase will likely require:

- Flyway migration for new company columns
- possible entity and DTO updates
- service-layer validation additions

If password change is added without schema changes:

- it should reuse the current user table and password hash field

---

## Testing Direction

This phase should include integration tests for:

- `GET /company` returning expanded fields
- `PUT /company` updating expanded company profile fields
- non-admin user denied company update
- password change success for authenticated user
- password change rejected for wrong current password
- password change rejected for invalid new password input

This phase should also verify:

- tenant-safe company update behavior
- no cross-user password change path exists

---

## Acceptance Criteria

This plan is complete when:

- company profile fields go beyond `name`
- desktop can fetch and save real company profile data
- authenticated users can change their own password
- backend settings APIs match the current desktop shell direction
- avatar/logo and theme persistence have an explicit deferred direction if not implemented now

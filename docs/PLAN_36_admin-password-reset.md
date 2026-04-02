# PLAN_36 Admin Password Reset

## Goal

Allow administrators to reset passwords for users they are authorized to manage, without introducing email-based recovery flows.

This plan is intentionally desktop-first and builds on the existing temporary-password onboarding pattern.

## Scope

### Backend

- Add password reset endpoint for `super-admin`
- Add password reset endpoint for `company-admin`
- Generate a new temporary password on reset
- Return the temporary password once in the response
- Enforce strict authorization:
  - `super-admin` can reset any tenant user
  - `company-admin` can reset users from the active company only
  - regular `user` cannot reset another user's password
- Prevent invalid reset targets if needed:
  - optional: prevent `company-admin` from resetting platform users
  - optional: prevent self-reset through admin flow

### Desktop

- Add reset-password action to:
  - `Super Admin Users` screen
  - company-admin `Users` screen
- Show confirmation before reset
- After successful reset, open one-time reveal dialog with the new temporary password
- Reuse the existing password-reveal UX pattern used after user creation

## API Direction

### Platform reset

- Endpoint under platform admin surface
- Example direction:
  - `POST /admin/users/{userId}/reset-password`

### Company reset

- Endpoint under tenant user-management surface
- Example direction:
  - `POST /users/{userId}/reset-password`

### Response

- `userId`
- `email`
- `temporaryPassword`

## UX Notes

- Reset should be a deliberate admin action:
  - confirm dialog required
- Temporary password must be clearly marked as:
  - shown once
  - copy now
- No email sending
- No token flow

## Authorization Rules

- `super-admin`
  - can reset passwords for tenant users across companies
- `company-admin`
  - can reset passwords only for users in the active company
- regular `user`
  - forbidden

## Out of Scope

- Self-service forgot password
- Email delivery
- Reset tokens
- Mandatory password rotation on next login
- Full audit/event history

## Verification

- Backend integration tests:
  - `super-admin` can reset target user password
  - `company-admin` can reset same-company user password
  - `company-admin` cannot reset other-company user password
  - regular `user` is forbidden
- Desktop verification:
  - reset action visible only to allowed admins
  - confirm dialog works
  - new temporary password is displayed once after success

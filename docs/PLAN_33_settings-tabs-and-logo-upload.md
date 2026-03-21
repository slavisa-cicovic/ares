# PLAN_33

## Goal

Refactor desktop `Settings` into a clearer two-tab structure and introduce the foundation for company logo upload using `RHFLogoPicker`.

## Scope

This plan should stay intentionally narrow:

- reorganize `Settings` into:
  - `Company`
  - `Profile`
- remove the current multi-section sprawl inside a single settings view
- prepare and implement the first real company logo upload flow
- keep the plan focused on settings structure and logo handling, without expanding into unrelated customization work

## Target UX

### 1. Settings tabs

The desktop settings screen should use two top-level tabs:

- `Company`
- `Profile`

Expected behavior:

- `Company` tab contains company identity and business settings
- `Profile` tab contains user-level account settings
- the screen should feel simpler than the current stacked settings layout

### 2. Company tab

The `Company` tab should be split into two cards:

- `Core`
- `Branding`

Each card should have its own:

- `Save`
- `Cancel`

The actions should sit in the card footer area, aligned bottom-right.

#### Core card

The `Core` card should own the business identity and contact data:

- company name
- PIB
- MB
- bank account
- email
- phone
- address
- postal code
- city
- country

Suggested row layout:

- row 1: `name`
- row 2: `PIB`, `MB`, `bankAccount`
- row 3: `email`, `phone`
- row 4: `address`
- row 5: `postalCode`, `city`, `country`

#### Branding card

The `Branding` card should own:

- logo
- theme preset
- brand color

For this plan, the biggest new behavior is:

- real logo upload flow

Suggested row layout:

- row 1: `logo`
- row 2: `theme`
- row 3: `color`

### 3. Profile tab

The `Profile` tab should own:

- current user information
- password change flow
- future place for avatar

Avatar upload does not need to be implemented yet unless it naturally falls out of the same upload infrastructure.

## Logo upload direction

### 1. Desktop form integration

Use:

- `desktop/src/components/hook-form/RHFLogoPicker.tsx`

as the form component for company logo selection.

Expected desktop behavior:

- choose image file
- preview selected logo
- allow removing/replacing it before save
- submit through company settings save flow

For brand color, this plan should also introduce:

- `desktop/src/components/hook-form/RHFColorPicker.tsx`

as a dedicated hook-form component rather than a raw input field.

### 2. Backend direction

This plan should define and implement one concrete backend approach for logo storage.

Pragmatic preferred direction for now:

- multipart upload endpoint or dedicated logo upload endpoint
- store uploaded file in a simple local/dev-friendly file storage location
- persist logo reference on company (`logoUrl` or similar existing field)

The plan should not jump directly to cloud/object-storage complexity unless needed.

### 3. Electron/desktop constraints

The desktop app should support file picking in a way that works cleanly in Electron.

If needed:

- use Electron bridge for native file selection
- keep renderer-side API simple

## Technical design

### Desktop

Likely files to update:

- `desktop/src/features/settings/SettingsScreen.tsx`
- `desktop/src/components/hook-form/RHFLogoPicker.tsx`
- `desktop/src/features/auth/AuthProvider.tsx`
- `desktop/src/lib/api/DesktopApiClient.ts`

Possible additions:

- `desktop/src/features/settings/CompanySettingsTab.tsx`
- `desktop/src/features/settings/ProfileSettingsTab.tsx`
- `desktop/src/features/settings/CompanyCoreCard.tsx`
- `desktop/src/features/settings/CompanyBrandingCard.tsx`

The company tab should use separate form state per card, not one shared form for both `Core` and `Branding`.

### Backend

Likely files to update/add:

- company controller/service layer
- upload endpoint
- static file serving or file download route
- company update/read DTOs if needed

This plan should also account for extending company core data if the backend does not yet expose:

- `postalCode`
- `city`
- `country`

## Validation and UX rules

- tabs must be clearly separated and stable
- save/cancel behavior must remain predictable
- invalid file types should be rejected with a clear message
- oversized images should be rejected or normalized with clear feedback
- logo preview should reflect the saved company logo after successful update

## Verification

### Desktop

- `npm run build`
- manually verify:
  - switching between `Company` and `Profile`
  - company field editing still works
  - password change still works
  - logo pick/preview/remove flow works

### Backend

- tests for company logo upload/update behavior
- tenant safety for logo update
- company read response returns updated logo reference

## Out of scope

Do not implement yet:

- avatar upload
- multiple logo variants
- image cropping UI
- PDF template customization
- full media library
- cloud/object storage support

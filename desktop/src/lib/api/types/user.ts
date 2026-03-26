export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

export type UserSummary = {
  id: string;
  companyId: string;
  companyName: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  roles: string[];
};

export type CreatePlatformUserPayload = {
  companyId: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  roles: string[];
};

export type CreateUserPayload = {
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  roles: string[];
};

export type CreatedPlatformUserResponse = UserSummary & {
  temporaryPassword: string;
};

export type CreatedUserResponse = UserSummary & {
  temporaryPassword: string;
};

export type UpdateOwnProfilePayload = {
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  userLogoUrl: string | null;
};

export type UserProfileResponse = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  userLogoUrl: string | null;
  status: string;
  roles: string[];
};

export type UserLogoUploadResponse = {
  logoUrl: string;
};

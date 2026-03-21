export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
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

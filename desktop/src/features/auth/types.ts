export type AuthenticatedUser = {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  userLogoUrl: string | null;
  companyId: string;
  companyName: string;
  companyBrandColor: string | null;
  companyLogoUrl: string | null;
  companyThemePreset: string;
  platformAdmin: boolean;
  roles: string[];
};

export type SessionData = {
  accessToken: string;
  refreshToken: string;
  user: AuthenticatedUser;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: AuthenticatedUser;
};

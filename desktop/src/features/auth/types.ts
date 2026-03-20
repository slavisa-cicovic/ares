export type AuthenticatedUser = {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  companyId: string;
  companyName: string;
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

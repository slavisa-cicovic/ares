export type CompanyResponse = {
  id: string;
  name: string;
  pib: string | null;
  mb: string | null;
  address: string | null;
  email: string | null;
  phone: string | null;
  bankAccount: string | null;
  status: string;
};

export type UpdateCompanyPayload = {
  name: string;
  pib: string | null;
  mb: string | null;
  address: string | null;
  email: string | null;
  phone: string | null;
  bankAccount: string | null;
};

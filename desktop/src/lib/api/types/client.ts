export type ClientStatus = "ACTIVE" | "INACTIVE";

export type ClientSummary = {
  id: string;
  name: string;
  pib: string | null;
  mb: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  status: ClientStatus;
};

export type ListClientsParams = {
  page: number;
  size: number;
  sort: string;
  status?: ClientStatus;
  search?: string;
};

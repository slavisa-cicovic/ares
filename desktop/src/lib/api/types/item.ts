export type ItemStatus = "ACTIVE" | "INACTIVE";

export type ItemSummary = {
  id: string;
  createdByUserId: string;
  name: string;
  code: string | null;
  description: string | null;
  unit: string;
  price: number;
  taxRate: number;
  status: ItemStatus;
};

export type UpsertItemPayload = {
  name: string;
  code: string | null;
  description: string | null;
  unit: string;
  price: string;
  taxRate: string;
  status: ItemStatus;
};

export type ListItemsParams = {
  page: number;
  size: number;
  sort: string;
  status?: ItemStatus;
  search?: string;
};

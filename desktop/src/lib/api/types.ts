export type ReportsSummary = {
  invoiceCount: number;
  totalInvoicedAmount: number;
  totalPaidAmount: number;
  totalOutstandingAmount: number;
  unpaidInvoiceCount: number;
  partiallyPaidInvoiceCount: number;
  paidInvoiceCount: number;
};

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

export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

export type ApiErrorPayload = {
  message?: string;
};

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
  status: string;
};

export type UpdateCompanyPayload = {
  name: string;
};

export type ApiErrorPayload = {
  message?: string;
};

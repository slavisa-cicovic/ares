export type ReportsSummary = {
  invoiceCount: number;
  totalInvoicedAmount: number;
  totalPaidAmount: number;
  totalOutstandingAmount: number;
  unpaidInvoiceCount: number;
  partiallyPaidInvoiceCount: number;
  paidInvoiceCount: number;
};

export type ApiErrorPayload = {
  message?: string;
};

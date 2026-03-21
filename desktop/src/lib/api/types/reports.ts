export type ReportsSummary = {
  invoiceCount: number;
  totalInvoicedAmount: number;
  totalPaidAmount: number;
  totalRefundedAmount: number;
  totalOutstandingAmount: number;
  unsettledRefundAmount: number;
  unpaidInvoiceCount: number;
  partiallyPaidInvoiceCount: number;
  paidInvoiceCount: number;
};

export type GetReportsSummaryParams = {
  from?: string;
  to?: string;
};

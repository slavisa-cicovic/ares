export type PaymentRecord = {
  id: string;
  paymentDate: string;
  amount: number;
  note: string | null;
};

export type PaymentSummary = {
  id: string;
  paymentDate: string;
  amount: number;
  note: string | null;
  invoiceId: string;
  invoiceNumber: string;
  invoiceIssueDate: string;
  clientId: string;
  clientName: string;
  currency: string;
};

export type ListPaymentsParams = {
  page: number;
  size: number;
  sort: string;
  search?: string;
  from?: string;
  to?: string;
};

export type CreatePaymentPayload = {
  paymentDate: string;
  amount: number;
  note: string | null;
};

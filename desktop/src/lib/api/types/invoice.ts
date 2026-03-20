export type InvoiceStatus = "DRAFT" | "ISSUED";

export type InvoicePaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID";

export type InvoiceSummary = {
  id: string;
  invoiceNumber: string;
  clientId: string;
  clientName: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  status: InvoiceStatus;
  subtotal: number;
  taxTotal: number;
  total: number;
  paymentStatus: InvoicePaymentStatus;
  paidAmount: number;
  outstandingAmount: number;
};

export type PaymentRecord = {
  id: string;
  paymentDate: string;
  amount: number;
  note: string | null;
};

export type InvoiceLine = {
  id: string;
  lineNumber: number;
  itemId: string | null;
  itemName: string;
  itemCode: string | null;
  unit: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  lineSubtotal: number;
  lineTaxTotal: number;
  lineTotal: number;
};

export type InvoiceDetail = {
  id: string;
  invoiceNumber: string;
  clientId: string;
  clientName: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  status: InvoiceStatus;
  note: string | null;
  subtotal: number;
  taxTotal: number;
  total: number;
  paymentStatus: InvoicePaymentStatus;
  paidAmount: number;
  outstandingAmount: number;
  lines: InvoiceLine[];
};

export type ListInvoicesParams = {
  page: number;
  size: number;
  sort: string;
  status?: InvoiceStatus;
  paymentStatus?: InvoicePaymentStatus;
  search?: string;
};

export type UpsertInvoiceLinePayload = {
  itemId: string | null;
  itemName: string;
  itemCode: string | null;
  unit: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
};

export type UpsertInvoicePayload = {
  clientId: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  note: string | null;
  lines: UpsertInvoiceLinePayload[];
};

export type CreatePaymentPayload = {
  paymentDate: string;
  amount: number;
  note: string | null;
};

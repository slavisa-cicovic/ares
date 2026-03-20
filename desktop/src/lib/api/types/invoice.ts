export type InvoiceStatus = "DRAFT" | "ISSUED";

export type InvoicePaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID";

export type InvoiceDocumentType = "INVOICE" | "CREDIT_NOTE";

export type InvoiceRefundStatus =
  | "NOT_APPLICABLE"
  | "NOT_REQUIRED"
  | "PENDING_REFUND"
  | "PARTIALLY_REFUNDED"
  | "REFUNDED";

export type InvoiceSummary = {
  id: string;
  invoiceNumber: string;
  clientId: string;
  clientName: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  status: InvoiceStatus;
  documentType: InvoiceDocumentType;
  originalInvoiceId: string | null;
  originalInvoiceNumber: string | null;
  linkedCreditNoteId: string | null;
  linkedCreditNoteNumber: string | null;
  subtotal: number;
  taxTotal: number;
  total: number;
  paymentStatus: InvoicePaymentStatus;
  paidAmount: number;
  outstandingAmount: number;
  refundStatus: InvoiceRefundStatus;
  refundableAmount: number;
  refundedAmount: number;
  remainingRefundAmount: number;
};

export type PaymentRecord = {
  id: string;
  paymentDate: string;
  amount: number;
  note: string | null;
};

export type RefundRecord = {
  id: string;
  refundDate: string;
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
  documentType: InvoiceDocumentType;
  originalInvoiceId: string | null;
  originalInvoiceNumber: string | null;
  linkedCreditNoteId: string | null;
  linkedCreditNoteNumber: string | null;
  note: string | null;
  subtotal: number;
  taxTotal: number;
  total: number;
  paymentStatus: InvoicePaymentStatus;
  paidAmount: number;
  outstandingAmount: number;
  refundStatus: InvoiceRefundStatus;
  refundableAmount: number;
  refundedAmount: number;
  remainingRefundAmount: number;
  lines: InvoiceLine[];
};

export type ListInvoicesParams = {
  page: number;
  size: number;
  sort: string;
  status?: InvoiceStatus;
  paymentStatus?: InvoicePaymentStatus;
  search?: string;
  from?: string;
  to?: string;
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

export type CreateRefundPayload = {
  refundDate: string;
  amount: number;
  note: string | null;
};

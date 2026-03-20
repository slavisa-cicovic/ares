import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from "@mui/material";
import { ConfirmDialog, CustomDialog } from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { InvoiceDetail, PaymentRecord, RefundRecord } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { PaymentDialog } from "./PaymentDialog";
import { RefundDialog } from "./RefundDialog";

type InvoiceDetailDialogProps = {
  invoiceId: string | null;
  open: boolean;
  onClose: () => void;
  canManagePayments: boolean;
  canCreateCreditNote: boolean;
  onChanged: () => void;
  onOpenInvoice: (invoiceId: string) => void;
};

function formatMoney(value: number, currency: string): string {
  return `${new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)} ${currency}`;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("sr-RS").format(new Date(`${value}T00:00:00`));
}

function statusColor(status: string): "default" | "warning" | "success" {
  if (status === "ISSUED") {
    return "success";
  }

  return "warning";
}

function paymentStatusColor(status: string): "default" | "warning" | "success" {
  if (status === "PAID") {
    return "success";
  }

  if (status === "PARTIALLY_PAID") {
    return "warning";
  }

  return "default";
}

function refundStatusColor(status: string): "default" | "warning" | "success" {
  if (status === "REFUNDED" || status === "NOT_REQUIRED") {
    return "success";
  }

  if (status === "PARTIALLY_REFUNDED" || status === "PENDING_REFUND") {
    return "warning";
  }

  return "default";
}

export function InvoiceDetailDialog({
  invoiceId,
  open,
  onClose,
  canManagePayments,
  canCreateCreditNote,
  onChanged,
  onOpenInvoice
}: InvoiceDetailDialogProps) {
  const { getInvoice, getInvoicePayments, getInvoiceRefunds, createInvoiceCreditNote } = useAuth();
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [refunds, setRefunds] = useState<RefundRecord[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isPaymentDialogOpen, setIsPaymentDialogOpen] = useState(false);
  const [isRefundDialogOpen, setIsRefundDialogOpen] = useState(false);
  const [isConfirmCreditNoteOpen, setIsConfirmCreditNoteOpen] = useState(false);
  const [isCreatingCreditNote, setIsCreatingCreditNote] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!open || !invoiceId) {
      return;
    }

    let active = true;
    setIsLoading(true);
    setError(null);

    void getInvoice(invoiceId)
      .then(async (invoiceResponse) => {
        const paymentResponse = invoiceResponse.documentType === "INVOICE"
          ? await getInvoicePayments(invoiceId)
          : [];
        const refundResponse = invoiceResponse.documentType === "CREDIT_NOTE"
          ? await getInvoiceRefunds(invoiceId)
          : [];

        if (active) {
          setInvoice(invoiceResponse);
          setPayments(paymentResponse);
          setRefunds(refundResponse);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError instanceof ApiClientError
              ? requestError.message
              : "Unable to load invoice details right now."
          );
          setInvoice(null);
          setPayments([]);
          setRefunds([]);
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [getInvoice, getInvoicePayments, getInvoiceRefunds, invoiceId, open, reloadKey]);

  return (
    <CustomDialog
      actions={
        <Stack direction="row" spacing={1.5}>
          {canCreateCreditNote &&
          invoice?.status === "ISSUED" &&
          invoice.documentType === "INVOICE" &&
          !invoice.linkedCreditNoteId ? (
            <Button
              onClick={() => setIsConfirmCreditNoteOpen(true)}
              variant="outlined"
            >
              Create Credit Note
            </Button>
          ) : null}

          {canManagePayments &&
          invoice?.status === "ISSUED" &&
          invoice.documentType === "INVOICE" &&
          invoice.outstandingAmount > 0 ? (
            <Button onClick={() => setIsPaymentDialogOpen(true)} variant="contained">
              Add Payment
            </Button>
          ) : null}

          {canManagePayments &&
          invoice?.documentType === "CREDIT_NOTE" &&
          invoice.remainingRefundAmount > 0 ? (
            <Button onClick={() => setIsRefundDialogOpen(true)} variant="contained">
              Record Refund
            </Button>
          ) : null}
        </Stack>
      }
      onClose={onClose}
      open={open}
      size="lg"
      title={invoice ? `Invoice ${invoice.invoiceNumber}` : "Invoice Details"}
    >
      {isLoading ? (
        <Typography color="text.secondary" variant="body2">
          Loading invoice details...
        </Typography>
      ) : null}

      {!isLoading && error ? <Alert severity="error">{error}</Alert> : null}

      {!isLoading && !error && invoice ? (
        <Stack spacing={3}>
          <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={2}>
            <Stack spacing={1}>
              <Typography variant="h6">{invoice.clientName}</Typography>
              <Typography color="text.secondary" variant="body2">
                Invoice No: {invoice.invoiceNumber}
              </Typography>
              {invoice.documentType === "CREDIT_NOTE" && invoice.originalInvoiceNumber ? (
                <Typography color="text.secondary" variant="body2">
                  Credit note for {invoice.originalInvoiceNumber}
                </Typography>
              ) : null}
              {invoice.documentType === "INVOICE" && invoice.linkedCreditNoteNumber ? (
                <Typography color="text.secondary" variant="body2">
                  Credit note: {invoice.linkedCreditNoteNumber}
                </Typography>
              ) : null}
            </Stack>

            <Stack direction="row" spacing={1}>
              <Chip color={statusColor(invoice.status)} label={invoice.status} size="small" />
              <Chip
                color={invoice.documentType === "CREDIT_NOTE" ? "default" : "primary"}
                label={invoice.documentType === "CREDIT_NOTE" ? "CREDIT NOTE" : "INVOICE"}
                size="small"
                variant={invoice.documentType === "CREDIT_NOTE" ? "outlined" : "filled"}
              />
              <Chip
                color={invoice.documentType === "CREDIT_NOTE"
                  ? refundStatusColor(invoice.refundStatus)
                  : paymentStatusColor(invoice.paymentStatus)}
                label={invoice.documentType === "CREDIT_NOTE" ? invoice.refundStatus : invoice.paymentStatus}
                size="small"
              />
            </Stack>
          </Stack>

          <Box
            sx={{
              display: "grid",
              gap: 2,
              gridTemplateColumns: { xs: "1fr", md: "repeat(3, minmax(0, 1fr))" }
            }}
          >
            <Info label="Issue Date" value={formatDate(invoice.issueDate)} />
            <Info label="Due Date" value={formatDate(invoice.dueDate)} />
            <Info label="Currency" value={invoice.currency} />
          </Box>

          {invoice.note ? (
            <>
              <Divider />
              <Stack spacing={0.75}>
                <Typography variant="subtitle2">Note</Typography>
                <Typography color="text.secondary" variant="body2">
                  {invoice.note}
                </Typography>
              </Stack>
            </>
          ) : null}

          <Divider />

          <Stack spacing={1.5}>
            <Typography variant="subtitle1">Line Items</Typography>

            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>#</TableCell>
                  <TableCell>Item</TableCell>
                  <TableCell>Unit</TableCell>
                  <TableCell align="right">Qty</TableCell>
                  <TableCell align="right">Unit Price</TableCell>
                  <TableCell align="right">Tax</TableCell>
                  <TableCell align="right">Line Total</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {invoice.lines.map((line) => (
                  <TableRow key={line.id}>
                    <TableCell>{line.lineNumber}</TableCell>
                    <TableCell>
                      <Stack spacing={0.25}>
                        <Typography variant="body2">{line.itemName}</Typography>
                        <Typography color="text.secondary" variant="caption">
                          {line.itemCode || "No code"}
                        </Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{line.unit}</TableCell>
                    <TableCell align="right">{line.quantity}</TableCell>
                    <TableCell align="right">{formatMoney(line.unitPrice, invoice.currency)}</TableCell>
                    <TableCell align="right">{line.taxRate} %</TableCell>
                    <TableCell align="right">{formatMoney(line.lineTotal, invoice.currency)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Stack>

          <Divider />

          <Stack spacing={1.5}>
            <Typography variant="subtitle1">{invoice.documentType === "CREDIT_NOTE" ? "Refunds" : "Payments"}</Typography>

            {invoice.documentType === "CREDIT_NOTE" ? (
              refunds.length === 0 ? (
                <Typography color="text.secondary" variant="body2">
                  No refunds recorded yet.
                </Typography>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Note</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {refunds.map((refund) => (
                      <TableRow key={refund.id}>
                        <TableCell>{formatDate(refund.refundDate)}</TableCell>
                        <TableCell align="right">{formatMoney(refund.amount, invoice.currency)}</TableCell>
                        <TableCell>{refund.note || "—"}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )
            ) : payments.length === 0 ? (
              <Typography color="text.secondary" variant="body2">
                No payments recorded yet.
              </Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Note</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {payments.map((payment) => (
                    <TableRow key={payment.id}>
                      <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                      <TableCell align="right">{formatMoney(payment.amount, invoice.currency)}</TableCell>
                      <TableCell>{payment.note || "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Stack>

          <Divider />

          <Box
            sx={{
              alignSelf: "flex-end",
              display: "grid",
              gap: 1,
              minWidth: { xs: "100%", md: 320 }
            }}
          >
            <TotalsRow label="Subtotal" value={formatMoney(invoice.subtotal, invoice.currency)} />
            <TotalsRow label="Tax" value={formatMoney(invoice.taxTotal, invoice.currency)} />
            <TotalsRow label="Total" value={formatMoney(invoice.total, invoice.currency)} />
            <TotalsRow label="Paid" value={formatMoney(invoice.paidAmount, invoice.currency)} />
            <TotalsRow label="Outstanding" value={formatMoney(invoice.outstandingAmount, invoice.currency)} strong />
            {invoice.documentType === "CREDIT_NOTE" ? (
              <>
                <TotalsRow label="Refundable" value={formatMoney(invoice.refundableAmount, invoice.currency)} />
                <TotalsRow label="Refunded" value={formatMoney(invoice.refundedAmount, invoice.currency)} />
                <TotalsRow label="Remaining Refund" value={formatMoney(invoice.remainingRefundAmount, invoice.currency)} strong />
              </>
            ) : null}
          </Box>
        </Stack>
      ) : null}

      <PaymentDialog
        currency={invoice?.currency ?? "RSD"}
        defaultAmount={invoice?.outstandingAmount ?? 0}
        invoiceId={invoiceId}
        onClose={() => setIsPaymentDialogOpen(false)}
        onSaved={() => {
          setReloadKey((current) => current + 1);
          onChanged();
        }}
        open={isPaymentDialogOpen}
      />
      <RefundDialog
        currency={invoice?.currency ?? "RSD"}
        defaultAmount={invoice?.remainingRefundAmount ?? 0}
        invoiceId={invoiceId}
        onClose={() => setIsRefundDialogOpen(false)}
        onSaved={() => {
          setReloadKey((current) => current + 1);
          onChanged();
        }}
        open={isRefundDialogOpen}
      />
      <ConfirmDialog
        confirmLabel={isCreatingCreditNote ? "Creating..." : "Create Credit Note"}
        content="This will create a separate credit note linked to the current issued invoice. Continue?"
        isLoading={isCreatingCreditNote}
        onClose={() => {
          if (!isCreatingCreditNote) {
            setIsConfirmCreditNoteOpen(false);
          }
        }}
        onConfirm={async () => {
          if (!invoiceId) {
            return;
          }

          try {
            setIsCreatingCreditNote(true);
            setError(null);
            const creditNote = await createInvoiceCreditNote(invoiceId);
            setIsConfirmCreditNoteOpen(false);
            onChanged();
            onOpenInvoice(creditNote.id);
          } catch (requestError) {
            setError(
              requestError instanceof ApiClientError
                ? requestError.message
                : "Unable to create a credit note right now."
            );
          } finally {
            setIsCreatingCreditNote(false);
          }
        }}
        open={isConfirmCreditNoteOpen}
        title="Create Credit Note"
      />
    </CustomDialog>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.5}>
      <Typography color="text.secondary" variant="caption">
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  );
}

function TotalsRow({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <Stack direction="row" justifyContent="space-between" spacing={2}>
      <Typography color={strong ? "text.primary" : "text.secondary"} fontWeight={strong ? 700 : 500} variant="body2">
        {label}
      </Typography>
      <Typography fontWeight={strong ? 700 : 600} variant="body2">
        {value}
      </Typography>
    </Stack>
  );
}

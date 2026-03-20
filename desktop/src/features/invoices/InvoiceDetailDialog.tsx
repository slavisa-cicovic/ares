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
import { CustomDialog } from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { InvoiceDetail, PaymentRecord } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { PaymentDialog } from "./PaymentDialog";

type InvoiceDetailDialogProps = {
  invoiceId: string | null;
  open: boolean;
  onClose: () => void;
  canManagePayments: boolean;
  onChanged: () => void;
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

export function InvoiceDetailDialog({ invoiceId, open, onClose, canManagePayments, onChanged }: InvoiceDetailDialogProps) {
  const { getInvoice, getInvoicePayments } = useAuth();
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isPaymentDialogOpen, setIsPaymentDialogOpen] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!open || !invoiceId) {
      return;
    }

    let active = true;
    setIsLoading(true);
    setError(null);

    void Promise.all([getInvoice(invoiceId), getInvoicePayments(invoiceId)])
      .then(([invoiceResponse, paymentResponse]) => {
        if (active) {
          setInvoice(invoiceResponse);
          setPayments(paymentResponse);
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
  }, [getInvoice, getInvoicePayments, invoiceId, open, reloadKey]);

  return (
    <CustomDialog
      actions={
        canManagePayments && invoice?.status === "ISSUED" && invoice.outstandingAmount > 0 ? (
          <Button onClick={() => setIsPaymentDialogOpen(true)} variant="contained">
            Add Payment
          </Button>
        ) : undefined
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
            </Stack>

            <Stack direction="row" spacing={1}>
              <Chip color={statusColor(invoice.status)} label={invoice.status} size="small" />
              <Chip color={paymentStatusColor(invoice.paymentStatus)} label={invoice.paymentStatus} size="small" />
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
            <Typography variant="subtitle1">Payments</Typography>

            {payments.length === 0 ? (
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

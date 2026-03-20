import { Divider, Stack, Typography } from "@mui/material";
import { CustomDialog } from "../../components";
import type { PaymentSummary } from "../../lib/api/types";

type PaymentDetailDialogProps = {
  open: boolean;
  payment: PaymentSummary | null;
  onClose: () => void;
};

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("sr-RS").format(new Date(`${value}T00:00:00`));
}

function formatMoney(value: number, currency: string): string {
  return `${new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)} ${currency}`;
}

export function PaymentDetailDialog({ open, payment, onClose }: PaymentDetailDialogProps) {
  return (
    <CustomDialog
      description="Review payment details, linked invoice, and client information."
      onClose={onClose}
      open={open}
      size="sm"
      title="Payment Details"
    >
      {payment ? (
        <Stack divider={<Divider flexItem />} spacing={2}>
          <DetailRow label="Payment Date" value={formatDate(payment.paymentDate)} />
          <DetailRow label="Amount" value={formatMoney(payment.amount, payment.currency)} />
          <DetailRow label="Invoice" value={payment.invoiceNumber} />
          <DetailRow label="Invoice Issue Date" value={formatDate(payment.invoiceIssueDate)} />
          <DetailRow label="Client" value={payment.clientName} />
          <DetailRow label="Note" value={payment.note?.trim() ? payment.note : "—"} />
        </Stack>
      ) : null}
    </CustomDialog>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.5}>
      <Typography color="text.secondary" variant="caption">
        {label}
      </Typography>
      <Typography variant="body1">{value}</Typography>
    </Stack>
  );
}

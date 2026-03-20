import { useEffect } from "react";
import { Alert, Button, Stack } from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { CustomDialog, FormProvider, RHFCurrencyField, RHFDatePicker, RHFTextField } from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { CreateRefundPayload } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { refundFormSchema, type RefundFormValues } from "./refund-form.schema";

type RefundDialogProps = {
  open: boolean;
  invoiceId: string | null;
  currency: string;
  defaultAmount: number;
  onClose: () => void;
  onSaved: () => void;
};

function toEmptyValues(defaultAmount: number): RefundFormValues {
  return {
    refundDate: new Date().toISOString().slice(0, 10),
    amount: defaultAmount > 0 ? defaultAmount.toFixed(2) : "",
    note: ""
  };
}

function toPayload(values: RefundFormValues): CreateRefundPayload {
  const trimmedNote = values.note.trim();

  return {
    refundDate: values.refundDate,
    amount: Number(values.amount.replace(",", ".")),
    note: trimmedNote ? trimmedNote : null
  };
}

export function RefundDialog({ open, invoiceId, currency, defaultAmount, onClose, onSaved }: RefundDialogProps) {
  const { createInvoiceRefund } = useAuth();
  const methods = useForm<RefundFormValues>({
    defaultValues: toEmptyValues(defaultAmount),
    resolver: zodResolver(refundFormSchema)
  });
  const {
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting }
  } = methods;

  useEffect(() => {
    if (open) {
      reset(toEmptyValues(defaultAmount));
    }
  }, [defaultAmount, open, reset]);

  const submitForm = handleSubmit(async (values) => {
    if (!invoiceId) {
      return;
    }

    try {
      await createInvoiceRefund(invoiceId, toPayload(values));
      onSaved();
      onClose();
    } catch (error) {
      const message =
        error instanceof ApiClientError ? error.message : "Unable to save refund right now.";
      setError("root", { message });
    }
  });

  return (
    <CustomDialog
      actions={
        <>
          <Button onClick={onClose} variant="text">
            Cancel
          </Button>
          <Button disabled={isSubmitting} onClick={() => void submitForm()} variant="contained">
            {isSubmitting ? "Saving..." : "Record Refund"}
          </Button>
        </>
      }
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      onClose={onClose}
      open={open}
      size="md"
      title="Record Refund"
    >
      <FormProvider methods={methods} onSubmit={(event) => void submitForm(event)}>
        <Stack spacing={2.5}>
          <RHFDatePicker label="Refund Date" name="refundDate" />
          <RHFCurrencyField currency={currency} label="Amount" name="amount" />
          <RHFTextField label="Note" multiline name="note" rows={3} />
          {errors.root?.message ? <Alert severity="error">{errors.root.message}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}

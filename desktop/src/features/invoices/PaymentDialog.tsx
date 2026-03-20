import { useEffect } from "react";
import { Alert, Button, Stack } from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { CustomDialog, FormProvider, RHFCurrencyField, RHFDatePicker, RHFTextField } from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { CreatePaymentPayload } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { paymentFormSchema, type PaymentFormValues } from "./payment-form.schema";

type PaymentDialogProps = {
  open: boolean;
  invoiceId: string | null;
  currency: string;
  defaultAmount: number;
  onClose: () => void;
  onSaved: () => void;
};

function toEmptyValues(defaultAmount: number): PaymentFormValues {
  return {
    paymentDate: new Date().toISOString().slice(0, 10),
    amount: defaultAmount > 0 ? defaultAmount.toFixed(2) : "",
    note: ""
  };
}

function toPayload(values: PaymentFormValues): CreatePaymentPayload {
  const trimmedNote = values.note.trim();

  return {
    paymentDate: values.paymentDate,
    amount: Number(values.amount),
    note: trimmedNote ? trimmedNote : null
  };
}

export function PaymentDialog({ open, invoiceId, currency, defaultAmount, onClose, onSaved }: PaymentDialogProps) {
  const { createInvoicePayment } = useAuth();
  const methods = useForm<PaymentFormValues>({
    defaultValues: toEmptyValues(defaultAmount),
    resolver: zodResolver(paymentFormSchema)
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
      await createInvoicePayment(invoiceId, toPayload(values));
      onSaved();
      onClose();
    } catch (error) {
      const message =
        error instanceof ApiClientError ? error.message : "Unable to save payment right now.";
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
            {isSubmitting ? "Saving..." : "Add Payment"}
          </Button>
        </>
      }
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      onClose={onClose}
      open={open}
      size="md"
      title="Add Payment"
    >
      <FormProvider methods={methods} onSubmit={(event) => void submitForm(event)}>
        <Stack spacing={2.5}>
          <RHFDatePicker label="Payment Date" name="paymentDate" />
          <RHFCurrencyField currency={currency} label="Amount" name="amount" />
          <RHFTextField label="Note" multiline name="note" rows={3} />
          {errors.root?.message ? <Alert severity="error">{errors.root.message}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}

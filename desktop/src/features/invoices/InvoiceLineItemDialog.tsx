import { useEffect } from "react";
import { Alert, Button, Stack } from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import {
  CustomDialog,
  FormProvider,
  RHFAutocomplete,
  RHFCurrencyField,
  RHFNumberField,
  RHFTextField
} from "../../components";
import type { ItemSummary } from "../../lib/api/types";
import { invoiceLineSchema, type InvoiceLineFormValues } from "./invoice-draft.schema";

type InvoiceLineItemDialogProps = {
  open: boolean;
  line: InvoiceLineFormValues | null;
  lineIndex: number | null;
  items: ItemSummary[];
  onClose: () => void;
  onSave: (line: InvoiceLineFormValues) => void;
};

const emptyValues: InvoiceLineFormValues = {
  itemId: null,
  itemName: "",
  itemCode: "",
  unit: "",
  quantity: "1",
  unitPrice: "",
  taxRate: "20"
};

export function InvoiceLineItemDialog({ open, line, lineIndex, items, onClose, onSave }: InvoiceLineItemDialogProps) {
  const methods = useForm<InvoiceLineFormValues>({
    defaultValues: line ?? emptyValues,
    resolver: zodResolver(invoiceLineSchema)
  });
  const {
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting }
  } = methods;

  useEffect(() => {
    if (open) {
      reset(line ?? emptyValues);
    }
  }, [line, open, reset]);

  function applySelectedItemDefaults(selectedItem: ItemSummary | null) {
    if (!selectedItem) {
      return;
    }

    setValue("itemName", selectedItem.name, { shouldDirty: true, shouldValidate: true });
    setValue("itemCode", selectedItem.code ?? "", { shouldDirty: true });
    setValue("unit", selectedItem.unit, { shouldDirty: true, shouldValidate: true });
    setValue("unitPrice", selectedItem.price.toFixed(2), { shouldDirty: true, shouldValidate: true });
    setValue("taxRate", selectedItem.taxRate.toFixed(0), { shouldDirty: true, shouldValidate: true });
  }

  const submitForm = handleSubmit(async (values) => {
    onSave(values);
  });

  return (
    <CustomDialog
      actions={
        <>
          <Button onClick={onClose} variant="text">
            Cancel
          </Button>
          <Button disabled={isSubmitting} onClick={() => void submitForm()} variant="contained">
            {line ? "Save Line" : "Add Line"}
          </Button>
        </>
      }
      onClose={onClose}
      open={open}
      size="md"
      title={line ? `Edit Line Item #${(lineIndex ?? 0) + 1}` : "Add Line Item"}
    >
      <FormProvider methods={methods} onSubmit={(event) => void submitForm(event)}>
        <Stack spacing={2.5}>
          <RHFAutocomplete
            getOptionLabel={(item) => item.name}
            getOptionValue={(item) => item.id}
            helperText="Choose an existing item or leave empty for manual entry."
            label="Item"
            name="itemId"
            onValueChange={applySelectedItemDefaults}
            options={items}
            placeholder="Start typing item name"
          />
          <RHFTextField label="Item Name" name="itemName" />
          <RHFTextField label="Item Code" name="itemCode" />
          <RHFTextField label="Unit" name="unit" />
          <RHFNumberField decimals={3} label="Quantity" name="quantity" />
          <RHFCurrencyField label="Unit Price" name="unitPrice" />
          <RHFNumberField decimals={0} label="Tax Rate" name="taxRate" suffix="%" />

          {errors.root?.message ? <Alert severity="error">{errors.root.message}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}

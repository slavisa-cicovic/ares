import { useEffect } from "react";
import { Alert, Button } from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import {
  CustomDialog,
  FormProvider,
  RHFCurrencyField,
  RHFNumberField,
  RHFSwitch,
  RHFTextField
} from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { ItemSummary, UpsertItemPayload } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { itemFormSchema, type ItemFormValues } from "./item-form.schema";

type ItemFormDialogProps = {
  open: boolean;
  item: ItemSummary | null;
  onClose: () => void;
  onSaved: () => void;
};

const emptyValues: ItemFormValues = {
  name: "",
  code: "",
  description: "",
  unit: "",
  price: "",
  taxRate: "",
  defaultValue: "1.000",
  active: true
};

function toFormValues(item: ItemSummary | null): ItemFormValues {
  if (!item) {
    return emptyValues;
  }

  return {
    name: item.name,
    code: item.code ?? "",
    description: item.description ?? "",
    unit: item.unit,
    price: item.price.toFixed(2),
    taxRate: item.taxRate.toFixed(0),
    defaultValue: item.defaultValue.toFixed(3),
    active: item.status === "ACTIVE"
  };
}

function toPayload(values: ItemFormValues): UpsertItemPayload {
  function normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  return {
    name: values.name.trim(),
    code: normalize(values.code),
    description: normalize(values.description),
    unit: values.unit.trim(),
    price: values.price.trim(),
    taxRate: values.taxRate.trim(),
    defaultValue: values.defaultValue.trim(),
    status: values.active ? "ACTIVE" : "INACTIVE"
  };
}

export function ItemFormDialog({ open, item, onClose, onSaved }: ItemFormDialogProps) {
  const { createItem, updateItem } = useAuth();
  const methods = useForm<ItemFormValues>({
    defaultValues: toFormValues(item),
    resolver: zodResolver(itemFormSchema)
  });

  const {
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting }
  } = methods;

  useEffect(() => {
    if (open) {
      reset(toFormValues(item));
    }
  }, [item, open, reset]);

  async function handleFormSubmit(values: ItemFormValues) {
    const payload = toPayload(values);

    try {
      if (item) {
        await updateItem(item.id, payload);
      } else {
        await createItem(payload);
      }

      onSaved();
      onClose();
    } catch (error) {
      const message =
        error instanceof ApiClientError ? error.message : "Unable to save item right now.";
      setError("root", { message });
    }
  }

  const submitForm = handleSubmit(handleFormSubmit);

  return (
    <CustomDialog
      actions={
        <>
          <Button onClick={onClose} variant="text">
            Cancel
          </Button>
          <Button
            disabled={isSubmitting}
            onClick={() => void submitForm()}
            variant="contained"
          >
            {isSubmitting ? "Saving..." : item ? "Save" : "Create"}
          </Button>
        </>
      }
      onClose={onClose}
      open={open}
      size="lg"
      title={item ? "Edit Item" : "New Item"}
    >
      <FormProvider
        className="client-form"
        methods={methods}
        onSubmit={(event) => void submitForm(event)}
      >
        <div className="client-form-grid">
          <RHFTextField helperText="Item or service name." label="Name" name="name" />
          <RHFSwitch
            className="client-form-switch"
            label="Item is active"
            name="active"
          />
          <RHFTextField label="Code" maxLength={64} name="code" />
          <RHFTextField label="Unit" maxLength={50} name="unit" />
          <RHFCurrencyField label="Price" name="price" />
          <RHFNumberField decimals={0} label="Tax Rate" name="taxRate" suffix="%" />
          <RHFNumberField decimals={3} label="Default Value" name="defaultValue" />
          <RHFTextField className="field-span-2" label="Description" multiline name="description" rows={3} />
        </div>

        {errors.root?.message ? (
          <Alert severity="error">{errors.root.message}</Alert>
        ) : null}
      </FormProvider>
    </CustomDialog>
  );
}

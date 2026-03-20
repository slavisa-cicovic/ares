import { useEffect, useMemo, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import DeleteOutlineRoundedIcon from "@mui/icons-material/DeleteOutlineRounded";
import {
  Alert,
  Box,
  Button,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Typography
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import {
  CustomDialog,
  FormProvider,
  RHFAutocomplete,
  RHFCurrencyField,
  RHFDatePicker,
  RHFNumberField,
  RHFSelect,
  RHFTextField
} from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type {
  ClientSummary,
  InvoiceDetail,
  ItemSummary,
  UpsertInvoicePayload
} from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { invoiceDraftSchema, type InvoiceDraftFormValues } from "./invoice-draft.schema";

type InvoiceDraftDialogProps = {
  open: boolean;
  invoiceId: string | null;
  onClose: () => void;
  onSaved: () => void;
};

const emptyLine = {
  itemId: null,
  itemName: "",
  itemCode: "",
  unit: "",
  quantity: "1",
  unitPrice: "",
  taxRate: "20"
};

const emptyValues: InvoiceDraftFormValues = {
  clientId: "",
  issueDate: new Date().toISOString().slice(0, 10),
  dueDate: new Date().toISOString().slice(0, 10),
  currency: "RSD",
  note: "",
  lines: [{ ...emptyLine }]
};

function formatMoney(value: number, currency: string): string {
  return `${new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)} ${currency}`;
}

function parseDecimal(value: string): number {
  if (!value.trim()) {
    return 0;
  }

  const normalized = value.replace(/\./g, "").replace(",", ".");
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
}

function toFormValues(invoice: InvoiceDetail | null): InvoiceDraftFormValues {
  if (!invoice) {
    return emptyValues;
  }

  return {
    clientId: invoice.clientId,
    issueDate: invoice.issueDate,
    dueDate: invoice.dueDate,
    currency: invoice.currency,
    note: invoice.note ?? "",
    lines: invoice.lines.map((line) => ({
      itemId: line.itemId,
      itemName: line.itemName,
      itemCode: line.itemCode ?? "",
      unit: line.unit,
      quantity: line.quantity.toFixed(3).replace(/\.?0+$/, ""),
      unitPrice: line.unitPrice.toFixed(2),
      taxRate: line.taxRate.toFixed(0)
    }))
  };
}

function toPayload(values: InvoiceDraftFormValues): UpsertInvoicePayload {
  function normalizeOptional(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  return {
    clientId: values.clientId,
    issueDate: values.issueDate,
    dueDate: values.dueDate,
    currency: values.currency.trim().toUpperCase(),
    note: normalizeOptional(values.note),
    lines: values.lines.map((line) => ({
      itemId: line.itemId,
      itemName: line.itemName.trim(),
      itemCode: normalizeOptional(line.itemCode),
      unit: line.unit.trim(),
      quantity: parseDecimal(line.quantity),
      unitPrice: parseDecimal(line.unitPrice),
      taxRate: parseDecimal(line.taxRate)
    }))
  };
}

export function InvoiceDraftDialog({ open, invoiceId, onClose, onSaved }: InvoiceDraftDialogProps) {
  const { getClients, getItems, getInvoice, createInvoice, updateInvoice } = useAuth();
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [items, setItems] = useState<ItemSummary[]>([]);
  const [isBootstrapping, setIsBootstrapping] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const methods = useForm<InvoiceDraftFormValues>({
    defaultValues: emptyValues,
    resolver: zodResolver(invoiceDraftSchema)
  });
  const {
    control,
    handleSubmit,
    reset,
    setError,
    setValue,
    formState: { errors, isSubmitting }
  } = methods;
  const { fields, append, remove } = useFieldArray({
    control,
    name: "lines"
  });
  const watchedLines = useWatch({ control, name: "lines" });
  const watchedCurrency = useWatch({ control, name: "currency" });

  const totalsPreview = useMemo(() => {
    const totals = (watchedLines ?? []).reduce(
      (accumulator, line) => {
        const quantity = parseDecimal(line?.quantity ?? "");
        const unitPrice = parseDecimal(line?.unitPrice ?? "");
        const taxRate = parseDecimal(line?.taxRate ?? "");
        const subtotal = quantity * unitPrice;
        const taxTotal = subtotal * (taxRate / 100);

        return {
          subtotal: accumulator.subtotal + subtotal,
          taxTotal: accumulator.taxTotal + taxTotal,
          total: accumulator.total + subtotal + taxTotal
        };
      },
      { subtotal: 0, taxTotal: 0, total: 0 }
    );

    return totals;
  }, [watchedLines]);

  useEffect(() => {
    if (!open) {
      return;
    }

    let active = true;
    setIsBootstrapping(true);
    setLoadError(null);

    void Promise.all([
      getClients({
        page: 0,
        size: 100,
        sort: "name,asc",
        status: "ACTIVE"
      }),
      getItems({
        page: 0,
        size: 100,
        sort: "name,asc",
        status: "ACTIVE"
      }),
      invoiceId ? getInvoice(invoiceId) : Promise.resolve(null)
    ])
      .then(([clientPage, itemPage, invoice]) => {
        if (!active) {
          return;
        }

        setClients(clientPage.items);
        setItems(itemPage.items);
        reset(toFormValues(invoice));
      })
      .catch((error) => {
        if (!active) {
          return;
        }

        setLoadError(
          error instanceof ApiClientError ? error.message : "Unable to load invoice editor."
        );
        reset(emptyValues);
      })
      .finally(() => {
        if (active) {
          setIsBootstrapping(false);
        }
      });

    return () => {
      active = false;
    };
  }, [getClients, getInvoice, getItems, invoiceId, open, reset]);

  useEffect(() => {
    (watchedLines ?? []).forEach((line, index) => {
      if (!line?.itemId) {
        return;
      }

      const selectedItem = items.find((item) => item.id === line.itemId);
      if (!selectedItem) {
        return;
      }

      if ((line.itemName ?? "") !== selectedItem.name) {
        setValue(`lines.${index}.itemName`, selectedItem.name, { shouldDirty: true, shouldValidate: true });
      }

      if ((line.itemCode ?? "") !== (selectedItem.code ?? "")) {
        setValue(`lines.${index}.itemCode`, selectedItem.code ?? "", { shouldDirty: true });
      }

      if ((line.unit ?? "") !== selectedItem.unit) {
        setValue(`lines.${index}.unit`, selectedItem.unit, { shouldDirty: true, shouldValidate: true });
      }

      const selectedPrice = selectedItem.price.toFixed(2);
      if ((line.unitPrice ?? "") !== selectedPrice) {
        setValue(`lines.${index}.unitPrice`, selectedPrice, { shouldDirty: true, shouldValidate: true });
      }

      const selectedTaxRate = selectedItem.taxRate.toFixed(0);
      if ((line.taxRate ?? "") !== selectedTaxRate) {
        setValue(`lines.${index}.taxRate`, selectedTaxRate, { shouldDirty: true, shouldValidate: true });
      }
    });
  }, [items, setValue, watchedLines]);

  async function handleFormSubmit(values: InvoiceDraftFormValues) {
    try {
      if (invoiceId) {
        await updateInvoice(invoiceId, toPayload(values));
      } else {
        await createInvoice(toPayload(values));
      }

      onSaved();
      onClose();
    } catch (error) {
      const message =
        error instanceof ApiClientError ? error.message : "Unable to save invoice draft right now.";
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
          <Button disabled={isSubmitting || isBootstrapping} onClick={() => void submitForm()} variant="contained">
            {isSubmitting ? "Saving..." : invoiceId ? "Save Draft" : "Create Draft"}
          </Button>
        </>
      }
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      onClose={onClose}
      open={open}
      size="lg"
      title={invoiceId ? "Edit Draft Invoice" : "New Draft Invoice"}
    >
      {isBootstrapping ? (
        <Typography color="text.secondary" variant="body2">
          Loading invoice editor...
        </Typography>
      ) : null}

      {!isBootstrapping && loadError ? <Alert severity="error">{loadError}</Alert> : null}

      {!isBootstrapping && !loadError ? (
        <FormProvider methods={methods} onSubmit={(event) => void submitForm(event)}>
          <Stack spacing={3}>
            <Box
              sx={{
                display: "grid",
                    gap: 2,
                    gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" }
                  }}
            >
              <RHFAutocomplete
                getOptionLabel={(client) => client.name}
                getOptionValue={(client) => client.id}
                helperText="Choose the invoice client."
                label="Client"
                name="clientId"
                options={clients}
                placeholder="Start typing client name"
              />
              <RHFSelect label="Currency" name="currency">
                <MenuItem value="RSD">RSD</MenuItem>
                <MenuItem value="EUR">EUR</MenuItem>
              </RHFSelect>
              <RHFDatePicker label="Issue Date" name="issueDate" />
              <RHFDatePicker label="Due Date" min={methods.watch("issueDate")} name="dueDate" />
              <RHFTextField className="field-span-2" label="Note" multiline name="note" rows={3} />
            </Box>

            <Divider />

            <Stack alignItems="center" direction="row" justifyContent="space-between" spacing={2}>
              <Typography variant="h6">Line Items</Typography>
              <Button
                onClick={() => append({ ...emptyLine })}
                startIcon={<AddRoundedIcon />}
                variant="outlined"
              >
                Add Line
              </Button>
            </Stack>

            <Stack spacing={2}>
              {fields.map((field, index) => (
                <Paper key={field.id} sx={{ p: 2.5 }} variant="outlined">
                  <Stack spacing={2}>
                    <Stack alignItems="center" direction="row" justifyContent="space-between" spacing={2}>
                      <Typography variant="subtitle1">Line {index + 1}</Typography>
                      <IconButton
                        color="error"
                        disabled={fields.length === 1}
                        onClick={() => remove(index)}
                      >
                        <DeleteOutlineRoundedIcon />
                      </IconButton>
                    </Stack>

                    <Box
                      sx={{
                        display: "grid",
                        gap: 2,
                        gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" }
                      }}
                    >
                      <RHFAutocomplete
                        getOptionLabel={(item) => item.name}
                        getOptionValue={(item) => item.id}
                        helperText="Choose an existing item or leave empty for manual entry."
                        label="Item"
                        name={`lines.${index}.itemId`}
                        options={items}
                        placeholder="Start typing item name"
                      />
                      <RHFTextField label="Item Name" name={`lines.${index}.itemName`} />
                      <RHFTextField label="Item Code" name={`lines.${index}.itemCode`} />
                      <RHFTextField label="Unit" name={`lines.${index}.unit`} />
                      <RHFNumberField decimals={3} label="Quantity" name={`lines.${index}.quantity`} />
                      <RHFCurrencyField label="Unit Price" name={`lines.${index}.unitPrice`} />
                      <RHFNumberField decimals={0} label="Tax Rate" name={`lines.${index}.taxRate`} suffix="%" />
                    </Box>
                  </Stack>
                </Paper>
              ))}
            </Stack>

            <Box sx={{ alignSelf: "flex-end", display: "grid", gap: 1, minWidth: { xs: "100%", md: 320 } }}>
              <TotalsRow label="Subtotal" value={formatMoney(totalsPreview.subtotal, watchedCurrency || "RSD")} />
              <TotalsRow label="Tax" value={formatMoney(totalsPreview.taxTotal, watchedCurrency || "RSD")} />
              <TotalsRow label="Total" strong value={formatMoney(totalsPreview.total, watchedCurrency || "RSD")} />
              <Typography color="text.secondary" variant="caption">
                Backend recalculates totals on save.
              </Typography>
            </Box>

            {errors.root?.message ? <Alert severity="error">{errors.root.message}</Alert> : null}
          </Stack>
        </FormProvider>
      ) : null}
    </CustomDialog>
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

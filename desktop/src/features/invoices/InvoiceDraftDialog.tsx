import { useEffect, useMemo, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import DeleteOutlineRoundedIcon from "@mui/icons-material/DeleteOutlineRounded";
import EditRoundedIcon from "@mui/icons-material/EditRounded";
import {
  Alert,
  Box,
  Button,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import {
  CustomDialog,
  FormProvider,
  RHFAutocomplete,
  RHFDatePicker,
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
import { InvoiceLineItemDialog } from "./InvoiceLineItemDialog";
import { invoiceDraftSchema, type InvoiceDraftFormValues, type InvoiceLineFormValues } from "./invoice-draft.schema";

type InvoiceDraftDialogProps = {
  open: boolean;
  invoiceId: string | null;
  onClose: () => void;
  onSaved: () => void;
};

const emptyLine: InvoiceLineFormValues = {
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

function formatPercent(value: string): string {
  return `${new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(parseDecimal(value))} %`;
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

function lineTotal(line: InvoiceLineFormValues): number {
  const quantity = parseDecimal(line.quantity);
  const unitPrice = parseDecimal(line.unitPrice);
  const taxRate = parseDecimal(line.taxRate);
  const subtotal = quantity * unitPrice;
  return subtotal + subtotal * (taxRate / 100);
}

export function InvoiceDraftDialog({ open, invoiceId, onClose, onSaved }: InvoiceDraftDialogProps) {
  const { getClients, getItems, getInvoice, createInvoice, updateInvoice } = useAuth();
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [items, setItems] = useState<ItemSummary[]>([]);
  const [isBootstrapping, setIsBootstrapping] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [lineDialogOpen, setLineDialogOpen] = useState(false);
  const [editingLineIndex, setEditingLineIndex] = useState<number | null>(null);
  const methods = useForm<InvoiceDraftFormValues>({
    defaultValues: emptyValues,
    resolver: zodResolver(invoiceDraftSchema)
  });
  const {
    getValues,
    handleSubmit,
    reset,
    setError,
    setValue,
    formState: { errors, isSubmitting }
  } = methods;
  const watchedLines = useWatch({ control: methods.control, name: "lines" });
  const watchedCurrency = useWatch({ control: methods.control, name: "currency" });

  const totalsPreview = useMemo(() => {
    return (watchedLines ?? []).reduce(
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

  function handleOpenNewLine() {
    setEditingLineIndex(null);
    setLineDialogOpen(true);
  }

  function handleOpenEditLine(index: number) {
    setEditingLineIndex(index);
    setLineDialogOpen(true);
  }

  function handleDeleteLine(index: number) {
    const nextLines = [...getValues("lines")];
    nextLines.splice(index, 1);
    setValue("lines", nextLines.length > 0 ? nextLines : [{ ...emptyLine }], {
      shouldDirty: true,
      shouldValidate: true
    });
  }

  function handleSaveLine(line: InvoiceLineFormValues) {
    const nextLines = [...getValues("lines")];

    if (editingLineIndex === null) {
      nextLines.push(line);
    } else {
      nextLines[editingLineIndex] = line;
    }

    setValue("lines", nextLines, {
      shouldDirty: true,
      shouldValidate: true
    });
    setEditingLineIndex(null);
    setLineDialogOpen(false);
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
              <Button onClick={handleOpenNewLine} startIcon={<AddRoundedIcon />} variant="outlined">
                Add Line
              </Button>
            </Stack>

            <Paper variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>#</TableCell>
                    <TableCell>Item</TableCell>
                    <TableCell>Code</TableCell>
                    <TableCell>Unit</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Price</TableCell>
                    <TableCell align="right">Tax</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {watchedLines?.map((line, index) => (
                    <TableRow key={`line-${index}`}>
                      <TableCell>{index + 1}</TableCell>
                      <TableCell>
                        <Stack spacing={0.25}>
                          <Typography variant="body2">{line.itemName || "Untitled line"}</Typography>
                          {line.itemId ? (
                            <Typography color="text.secondary" variant="caption">
                              Existing item
                            </Typography>
                          ) : null}
                        </Stack>
                      </TableCell>
                      <TableCell>{line.itemCode || "—"}</TableCell>
                      <TableCell>{line.unit || "—"}</TableCell>
                      <TableCell align="right">{line.quantity || "—"}</TableCell>
                      <TableCell align="right">{line.unitPrice ? formatMoney(parseDecimal(line.unitPrice), watchedCurrency || "RSD") : "—"}</TableCell>
                      <TableCell align="right">{line.taxRate ? formatPercent(line.taxRate) : "—"}</TableCell>
                      <TableCell align="right">{formatMoney(lineTotal(line), watchedCurrency || "RSD")}</TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end" spacing={0.5}>
                          <IconButton onClick={() => handleOpenEditLine(index)} size="small">
                            <EditRoundedIcon fontSize="small" />
                          </IconButton>
                          <IconButton color="error" disabled={(watchedLines?.length ?? 0) === 1} onClick={() => handleDeleteLine(index)} size="small">
                            <DeleteOutlineRoundedIcon fontSize="small" />
                          </IconButton>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>

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

      <InvoiceLineItemDialog
        items={items}
        line={editingLineIndex === null ? null : (watchedLines?.[editingLineIndex] ?? null)}
        lineIndex={editingLineIndex}
        onClose={() => {
          setEditingLineIndex(null);
          setLineDialogOpen(false);
        }}
        onSave={handleSaveLine}
        open={lineDialogOpen}
      />
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

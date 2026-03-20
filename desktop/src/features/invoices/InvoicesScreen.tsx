import { useEffect, useState } from "react";
import ReceiptLongRoundedIcon from "@mui/icons-material/ReceiptLongRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  type SelectChangeEvent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Typography
} from "@mui/material";
import { emptyRows, useTable } from "../../components/table";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type {
  InvoicePaymentStatus,
  InvoiceStatus,
  InvoiceSummary,
  PagedResponse
} from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import type { AuthenticatedUser } from "../auth/types";
import { InvoiceDetailDialog } from "./InvoiceDetailDialog";
import { InvoiceDraftDialog } from "./InvoiceDraftDialog";

const headCells = [
  { id: "invoiceNumber", label: "Invoice", minWidth: 160, sortable: true, align: undefined },
  { id: "clientName", label: "Client", minWidth: 220, sortable: false, align: undefined },
  { id: "issueDate", label: "Issue Date", minWidth: 140, sortable: true, align: undefined },
  { id: "dueDate", label: "Due Date", minWidth: 140, sortable: true, align: undefined },
  { id: "status", label: "Status", minWidth: 120, sortable: false, align: undefined },
  { id: "paymentStatus", label: "Payment", minWidth: 140, sortable: false, align: undefined },
  { id: "total", label: "Total", minWidth: 140, sortable: true, align: "right" as const },
  { id: "outstandingAmount", label: "Outstanding", minWidth: 150, sortable: false, align: "right" as const },
  { id: "actions", label: "Actions", minWidth: 120, sortable: false, align: "right" as const }
] as const;

const emptyPage: PagedResponse<InvoiceSummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "invoiceNumber" || orderBy === "issueDate" || orderBy === "dueDate" || orderBy === "total") {
    return `${orderBy},${order}`;
  }

  return "issueDate,desc";
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("sr-RS").format(new Date(`${value}T00:00:00`));
}

function formatMoney(value: number, currency: string): string {
  return `${new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)} ${currency}`;
}

function statusColor(status: InvoiceStatus): "warning" | "success" {
  return status === "ISSUED" ? "success" : "warning";
}

function paymentStatusColor(status: InvoicePaymentStatus): "default" | "warning" | "success" {
  if (status === "PAID") {
    return "success";
  }

  if (status === "PARTIALLY_PAID") {
    return "warning";
  }

  return "default";
}

function canEditDraft(user: AuthenticatedUser, invoice: InvoiceSummary): boolean {
  return user.roles.includes("company-admin") && invoice.status === "DRAFT";
}

export function InvoicesScreen() {
  const { getInvoices, state } = useAuth();
  const table = useTable({
    defaultOrderBy: "issueDate",
    defaultOrder: "desc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | InvoiceStatus>("");
  const [paymentStatus, setPaymentStatus] = useState<"" | InvoicePaymentStatus>("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<InvoiceSummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(null);
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | null>(null);
  const [isDraftDialogOpen, setIsDraftDialogOpen] = useState(false);

  if (state.status !== "authenticated") {
    return null;
  }

  const user = state.user;
  const isCompanyAdmin = user.roles.includes("company-admin");

  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    void getInvoices({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
      status: status || undefined,
      paymentStatus: paymentStatus || undefined,
      search
    })
      .then((response) => {
        if (active) {
          setPageData(response);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError instanceof ApiClientError
              ? requestError.message
              : "Unable to load invoices right now."
          );
          setPageData(emptyPage);
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
  }, [getInvoices, paymentStatus, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage]);

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Invoices
        </Typography>
        <Typography variant="h3">Invoice overview</Typography>
        <Typography color="text.secondary" variant="body1">
          Browse invoices, inspect payment state, and manage draft invoices.
        </Typography>
      </Box>

      <Paper sx={{ p: 3 }} variant="outlined">
        <Stack spacing={3}>
          <Stack
            alignItems={{ md: "flex-end" }}
            direction={{ xs: "column", md: "row" }}
            justifyContent="space-between"
            spacing={2}
          >
            <Stack direction={{ xs: "column", md: "row" }} spacing={2} sx={{ flex: 1 }}>
              <TextField
                fullWidth
                label="Search"
                onChange={(event) => {
                  setSearch(event.target.value);
                  table.onResetPage();
                }}
                placeholder="Search by invoice number or client"
                value={search}
              />

              <FormControl sx={{ minWidth: 160 }}>
                <InputLabel id="invoice-status-label">Status</InputLabel>
                <Select
                  label="Status"
                  labelId="invoice-status-label"
                  onChange={(event: SelectChangeEvent<"" | InvoiceStatus>) => {
                    setStatus(event.target.value as "" | InvoiceStatus);
                    table.onResetPage();
                  }}
                  value={status}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="DRAFT">Draft</MenuItem>
                  <MenuItem value="ISSUED">Issued</MenuItem>
                </Select>
              </FormControl>

              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="invoice-payment-status-label">Payment</InputLabel>
                <Select
                  label="Payment"
                  labelId="invoice-payment-status-label"
                  onChange={(event: SelectChangeEvent<"" | InvoicePaymentStatus>) => {
                    setPaymentStatus(event.target.value as "" | InvoicePaymentStatus);
                    table.onResetPage();
                  }}
                  value={paymentStatus}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="UNPAID">Unpaid</MenuItem>
                  <MenuItem value="PARTIALLY_PAID">Partially Paid</MenuItem>
                  <MenuItem value="PAID">Paid</MenuItem>
                </Select>
              </FormControl>
            </Stack>

            <Stack direction="row" spacing={1.5}>
              <Button onClick={() => setReloadKey((current) => current + 1)} startIcon={<RefreshRoundedIcon />} variant="outlined">
                Refresh
              </Button>
              {isCompanyAdmin ? (
                <Button
                  onClick={() => {
                    setEditingInvoiceId(null);
                    setIsDraftDialogOpen(true);
                  }}
                  startIcon={<ReceiptLongRoundedIcon />}
                  variant="contained"
                >
                  New Invoice
                </Button>
              ) : null}
            </Stack>
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Typography color="text.secondary" variant="body2">
            {totalElements} {totalElements === 1 ? "invoice" : "invoices"} in this company
          </Typography>

          <Paper sx={{ overflow: "hidden" }} variant="outlined">
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headCells.map((cell) => (
                      <TableCell align={cell.align} key={cell.id} sx={{ minWidth: cell.minWidth }}>
                        {cell.sortable ? (
                          <TableSortLabel
                            active={table.orderBy === cell.id}
                            direction={table.orderBy === cell.id ? table.order : "asc"}
                            onClick={() => table.onSort(cell.id)}
                          >
                            {cell.label}
                          </TableSortLabel>
                        ) : (
                          cell.label
                        )}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {isLoading
                    ? Array.from({ length: table.rowsPerPage }).map((_, index) => (
                        <TableRow key={`skeleton-${index}`}>
                          <TableCell colSpan={headCells.length}>
                            <Box sx={{ py: 1 }}>
                              <div className="ui-table-skeleton" />
                            </Box>
                          </TableCell>
                        </TableRow>
                      ))
                    : null}

                  {!isLoading && rows.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={headCells.length}>
                        <Typography align="center" color="text.secondary" sx={{ py: 4 }} variant="body2">
                          {search || status || paymentStatus
                            ? "No invoices match the current filters."
                            : "No invoices have been created yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((invoice) => (
                        <TableRow hover key={invoice.id}>
                          <TableCell>
                            <Stack spacing={0.5}>
                              <Typography fontWeight={600} variant="body2">
                                {invoice.invoiceNumber}
                              </Typography>
                              <Typography color="text.secondary" variant="caption">
                                {invoice.clientName}
                              </Typography>
                            </Stack>
                          </TableCell>
                          <TableCell>{invoice.clientName}</TableCell>
                          <TableCell>{formatDate(invoice.issueDate)}</TableCell>
                          <TableCell>{formatDate(invoice.dueDate)}</TableCell>
                          <TableCell>
                            <Chip color={statusColor(invoice.status)} label={invoice.status} size="small" />
                          </TableCell>
                          <TableCell>
                            <Chip color={paymentStatusColor(invoice.paymentStatus)} label={invoice.paymentStatus} size="small" />
                          </TableCell>
                          <TableCell align="right">{formatMoney(invoice.total, invoice.currency)}</TableCell>
                          <TableCell align="right">{formatMoney(invoice.outstandingAmount, invoice.currency)}</TableCell>
                          <TableCell align="right">
                            <Stack direction="row" justifyContent="flex-end" spacing={1}>
                              <Button
                                onClick={() => setSelectedInvoiceId(invoice.id)}
                                size="small"
                                startIcon={<ReceiptLongRoundedIcon />}
                                variant="text"
                              >
                                Details
                              </Button>
                              {canEditDraft(user, invoice) ? (
                                <Button
                                  onClick={() => {
                                    setEditingInvoiceId(invoice.id);
                                    setIsDraftDialogOpen(true);
                                  }}
                                  size="small"
                                  variant="text"
                                >
                                  Edit
                                </Button>
                              ) : null}
                            </Stack>
                          </TableCell>
                        </TableRow>
                      ))
                    : null}

                  {!isLoading && emptyRowsCount > 0
                    ? Array.from({ length: emptyRowsCount }).map((_, index) => (
                        <TableRow key={`empty-${index}`} sx={{ height: table.dense ? 52 : 66 }}>
                          <TableCell colSpan={headCells.length} />
                        </TableRow>
                      ))
                    : null}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>

          <TablePagination
            component="div"
            count={totalElements}
            onPageChange={(_event, newPage) => table.onChangePage(newPage)}
            onRowsPerPageChange={table.onChangeRowsPerPage}
            page={table.page}
            rowsPerPage={table.rowsPerPage}
            rowsPerPageOptions={[10, 20, 50]}
          />
        </Stack>
      </Paper>

      <InvoiceDetailDialog
        invoiceId={selectedInvoiceId}
        onClose={() => setSelectedInvoiceId(null)}
        open={Boolean(selectedInvoiceId)}
      />
      <InvoiceDraftDialog
        invoiceId={editingInvoiceId}
        onClose={() => {
          setEditingInvoiceId(null);
          setIsDraftDialogOpen(false);
        }}
        onSaved={() => {
          setReloadKey((current) => current + 1);
        }}
        open={isDraftDialogOpen}
      />
    </Stack>
  );
}

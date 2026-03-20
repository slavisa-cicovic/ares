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
import { ConfirmDialog, DateRangeFilter, FiltersResult, type FilterItem } from "../../components";
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

function hasActiveFilters(
  search: string,
  status: "" | InvoiceStatus,
  paymentStatus: "" | InvoicePaymentStatus,
  from: string,
  to: string
): boolean {
  return Boolean(search || status || paymentStatus || from || to);
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

function refundStatusColor(status: string): "default" | "warning" | "success" {
  if (status === "REFUNDED" || status === "NOT_REQUIRED") {
    return "success";
  }

  if (status === "PARTIALLY_REFUNDED" || status === "PENDING_REFUND") {
    return "warning";
  }

  return "default";
}

function canEditDraft(user: AuthenticatedUser, invoice: InvoiceSummary): boolean {
  return user.roles.includes("company-admin") && invoice.status === "DRAFT" && invoice.documentType === "INVOICE";
}

export function InvoicesScreen() {
  const { getInvoices, issueInvoice, state } = useAuth();
  const table = useTable({
    defaultOrderBy: "issueDate",
    defaultOrder: "desc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | InvoiceStatus>("");
  const [paymentStatus, setPaymentStatus] = useState<"" | InvoicePaymentStatus>("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<InvoiceSummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(null);
  const [editingInvoiceId, setEditingInvoiceId] = useState<string | null>(null);
  const [isDraftDialogOpen, setIsDraftDialogOpen] = useState(false);
  const [issueInvoiceId, setIssueInvoiceId] = useState<string | null>(null);
  const [isIssuing, setIsIssuing] = useState(false);

  if (state.status !== "authenticated") {
    return null;
  }

  const user = state.user;
  const isCompanyAdmin = user.roles.includes("company-admin");

  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);
  const hasInvalidDateRange = Boolean(from && to && from > to);
  const filters: FilterItem[] = [
    ...(search
      ? [
          {
            label: "Search",
            value: search,
            onRemove: () => {
              setSearch("");
              table.onResetPage();
            }
          }
        ]
      : []),
    ...(status
      ? [
          {
            label: "Status",
            value: status,
            onRemove: () => {
              setStatus("");
              table.onResetPage();
            }
          }
        ]
      : []),
    ...(paymentStatus
      ? [
          {
            label: "Payment",
            value: paymentStatus,
            onRemove: () => {
              setPaymentStatus("");
              table.onResetPage();
            }
          }
        ]
      : []),
    ...(from
      ? [
          {
            label: "From",
            value: formatDate(from),
            onRemove: () => {
              setFrom("");
              table.onResetPage();
            }
          }
        ]
      : []),
    ...(to
      ? [
          {
            label: "To",
            value: formatDate(to),
            onRemove: () => {
              setTo("");
              table.onResetPage();
            }
          }
        ]
      : [])
  ];

  function blurActiveElement() {
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }

  function openDialog(openAction: () => void) {
    blurActiveElement();
    openAction();
  }

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    if (hasInvalidDateRange) {
      setError("From date must be earlier than or equal to To date.");
      setPageData(emptyPage);
      setIsLoading(false);
      return () => {
        active = false;
      };
    }

    void getInvoices({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
      status: status || undefined,
      paymentStatus: paymentStatus || undefined,
      search,
      from: from || undefined,
      to: to || undefined
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
  }, [from, getInvoices, hasInvalidDateRange, paymentStatus, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage, to]);

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
            <Stack direction={{ xs: "column", md: "row" }} spacing={2} sx={{ flex: 1, flexWrap: "wrap" }}>
              <TextField
                label="Search"
                onChange={(event) => {
                  setSearch(event.target.value);
                  table.onResetPage();
                }}
                placeholder="Search by invoice number or client"
                sx={{ minWidth: 280, maxWidth: 360, width: "100%" }}
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

              <DateRangeFilter
                from={from}
                onFromChange={(value) => {
                  setFrom(value);
                  table.onResetPage();
                }}
                onToChange={(value) => {
                  setTo(value);
                  table.onResetPage();
                }}
                to={to}
              />
            </Stack>

            <Stack direction="row" spacing={1.5}>
              <Button onClick={() => setReloadKey((current) => current + 1)} startIcon={<RefreshRoundedIcon />} variant="outlined">
                Refresh
              </Button>
              {isCompanyAdmin ? (
                <Button
                  onClick={() => {
                    blurActiveElement();
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

          <FiltersResult
            filters={filters}
            onReset={() => {
              setSearch("");
              setStatus("");
              setPaymentStatus("");
              setFrom("");
              setTo("");
              table.onResetPage();
            }}
            total={totalElements}
          />

          {error ? <Alert severity="error">{error}</Alert> : null}

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
                          {hasActiveFilters(search, status, paymentStatus, from, to)
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
                              {invoice.documentType === "CREDIT_NOTE" && invoice.originalInvoiceNumber ? (
                                <Typography color="text.secondary" variant="caption">
                                  Credit note for {invoice.originalInvoiceNumber}
                                </Typography>
                              ) : null}
                            </Stack>
                          </TableCell>
                          <TableCell>{invoice.clientName}</TableCell>
                          <TableCell>{formatDate(invoice.issueDate)}</TableCell>
                          <TableCell>{formatDate(invoice.dueDate)}</TableCell>
                          <TableCell>
                            <Chip color={statusColor(invoice.status)} label={invoice.status} size="small" />
                          </TableCell>
                          <TableCell>
                            <Chip
                              color={invoice.documentType === "CREDIT_NOTE"
                                ? refundStatusColor(invoice.refundStatus)
                                : paymentStatusColor(invoice.paymentStatus)}
                              label={invoice.documentType === "CREDIT_NOTE" ? invoice.refundStatus : invoice.paymentStatus}
                              size="small"
                            />
                          </TableCell>
                          <TableCell align="right">{formatMoney(invoice.total, invoice.currency)}</TableCell>
                          <TableCell align="right">{formatMoney(invoice.outstandingAmount, invoice.currency)}</TableCell>
                          <TableCell align="right">
                            <Stack direction="row" justifyContent="flex-end" spacing={1}>
                              <Button
                                onClick={() => openDialog(() => setSelectedInvoiceId(invoice.id))}
                                size="small"
                                startIcon={<ReceiptLongRoundedIcon />}
                                variant="text"
                              >
                                Details
                              </Button>
                              {canEditDraft(user, invoice) ? (
                                <>
                                  <Button
                                    onClick={() => {
                                      blurActiveElement();
                                      setEditingInvoiceId(invoice.id);
                                      setIsDraftDialogOpen(true);
                                    }}
                                    size="small"
                                    variant="text"
                                  >
                                    Edit
                                  </Button>
                                  <Button
                                    color="warning"
                                    onClick={() => openDialog(() => setIssueInvoiceId(invoice.id))}
                                    size="small"
                                    variant="text"
                                  >
                                    Issue
                                  </Button>
                                </>
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
        canManagePayments={isCompanyAdmin}
        canCreateCreditNote={isCompanyAdmin}
        invoiceId={selectedInvoiceId}
        onChanged={() => {
          setReloadKey((current) => current + 1);
        }}
        onClose={() => setSelectedInvoiceId(null)}
        onOpenInvoice={(invoiceId) => openDialog(() => setSelectedInvoiceId(invoiceId))}
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
      <ConfirmDialog
        confirmColor="warning"
        confirmLabel="Issue Invoice"
        content="Issued invoices become read-only. Continue?"
        isLoading={isIssuing}
        onClose={() => {
          if (!isIssuing) {
            setIssueInvoiceId(null);
          }
        }}
        onConfirm={async () => {
          if (!issueInvoiceId) {
            return;
          }

          try {
            setIsIssuing(true);
            await issueInvoice(issueInvoiceId);
            setIssueInvoiceId(null);
            setReloadKey((current) => current + 1);
          } finally {
            setIsIssuing(false);
          }
        }}
        open={Boolean(issueInvoiceId)}
        title="Issue Invoice"
      />
    </Stack>
  );
}

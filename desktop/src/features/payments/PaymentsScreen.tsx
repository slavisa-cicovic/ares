import { useEffect, useState } from "react";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import VisibilityRoundedIcon from "@mui/icons-material/VisibilityRounded";
import {
  Alert,
  Box,
  Button,
  Paper,
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
import {
  DateRangeFilter,
  FiltersResult,
  formatDateRangePresetLabel,
  type DateRangePreset,
  type FilterItem
} from "../../components";
import { emptyRows, useTable } from "../../components/table";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { PagedResponse, PaymentSummary } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { PaymentDetailDialog } from "./PaymentDetailDialog";

const headCells = [
  { id: "paymentDate", label: "Payment Date", minWidth: 140, sortable: true, align: undefined },
  { id: "invoiceNumber", label: "Invoice", minWidth: 160, sortable: false, align: undefined },
  { id: "clientName", label: "Client", minWidth: 220, sortable: false, align: undefined },
  { id: "amount", label: "Amount", minWidth: 140, sortable: true, align: "right" as const },
  { id: "note", label: "Note", minWidth: 260, sortable: false, align: undefined },
  { id: "actions", label: "Actions", minWidth: 120, sortable: false, align: "right" as const }
] as const;

const emptyPage: PagedResponse<PaymentSummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "paymentDate" || orderBy === "amount") {
    return `${orderBy},${order}`;
  }

  return "paymentDate,desc";
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

function hasActiveFilters(search: string, from: string, to: string): boolean {
  return Boolean(search || from || to);
}

export function PaymentsScreen() {
  const { getPayments } = useAuth();
  const table = useTable({
    defaultOrderBy: "paymentDate",
    defaultOrder: "desc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [dateRangePreset, setDateRangePreset] = useState<DateRangePreset | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<PaymentSummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPayment, setSelectedPayment] = useState<PaymentSummary | null>(null);

  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);
  const hasInvalidDateRange = Boolean(from && to && from > to);

  const filters: FilterItem[] = [
    ...(search
      ? [{
          label: "Search",
          value: search,
          onRemove: () => {
            setSearch("");
            table.onResetPage();
          }
        }]
      : []),
    ...(dateRangePreset !== null && dateRangePreset !== "custom" && from && to
      ? [{
          label: "Range",
          value: formatDateRangePresetLabel(dateRangePreset),
          onRemove: () => {
            setDateRangePreset(null);
            setFrom("");
            setTo("");
            table.onResetPage();
          }
        }]
      : []),
    ...(dateRangePreset === "custom" && from
      ? [{
          label: "From",
          value: formatDate(from),
          onRemove: () => {
            setFrom("");
            table.onResetPage();
          }
        }]
      : []),
    ...(dateRangePreset === "custom" && to
      ? [{
          label: "To",
          value: formatDate(to),
          onRemove: () => {
            setTo("");
            table.onResetPage();
          }
        }]
      : [])
  ];

  function blurActiveElement() {
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
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

    void getPayments({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
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
              : "Unable to load payments right now."
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
  }, [from, getPayments, hasInvalidDateRange, reloadKey, search, table.order, table.orderBy, table.page, table.rowsPerPage, to]);

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Payments
        </Typography>
        <Typography variant="h3">Payment overview</Typography>
        <Typography color="text.secondary" variant="body1">
          Browse recorded payments across invoices and inspect collection history.
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
                placeholder="Search by invoice, client, or note"
                sx={{ minWidth: 280, maxWidth: 360, width: "100%" }}
                value={search}
              />

              <DateRangeFilter
                from={from}
                onFromChange={(value) => {
                  setFrom(value);
                  table.onResetPage();
                }}
                onPresetChange={(value) => {
                  setDateRangePreset(value);
                  table.onResetPage();
                }}
                onToChange={(value) => {
                  setTo(value);
                  table.onResetPage();
                }}
                preset={dateRangePreset}
                to={to}
              />
            </Stack>

            <Button onClick={() => setReloadKey((current) => current + 1)} startIcon={<RefreshRoundedIcon />} variant="outlined">
              Refresh
            </Button>
          </Stack>

          <FiltersResult
            filters={filters}
            onReset={() => {
              setSearch("");
              setDateRangePreset(null);
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
                          {hasActiveFilters(search, from, to)
                            ? "No payments match the current filters."
                            : "No payments have been recorded yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((payment) => (
                        <TableRow hover key={payment.id}>
                          <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                          <TableCell>
                            <Stack spacing={0.5}>
                              <Typography fontWeight={600} variant="body2">
                                {payment.invoiceNumber}
                              </Typography>
                              <Typography color="text.secondary" variant="caption">
                                Issued {formatDate(payment.invoiceIssueDate)}
                              </Typography>
                            </Stack>
                          </TableCell>
                          <TableCell>{payment.clientName}</TableCell>
                          <TableCell align="right">{formatMoney(payment.amount, payment.currency)}</TableCell>
                          <TableCell>{payment.note?.trim() ? payment.note : "—"}</TableCell>
                          <TableCell align="right">
                            <Button
                              onClick={() => {
                                blurActiveElement();
                                setSelectedPayment(payment);
                              }}
                              size="small"
                              startIcon={<VisibilityRoundedIcon />}
                              variant="text"
                            >
                              Details
                            </Button>
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

      <PaymentDetailDialog
        onClose={() => setSelectedPayment(null)}
        open={Boolean(selectedPayment)}
        payment={selectedPayment}
      />
    </Stack>
  );
}

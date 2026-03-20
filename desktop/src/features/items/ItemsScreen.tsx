import { useEffect, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import DeleteOutlineRoundedIcon from "@mui/icons-material/DeleteOutlineRounded";
import EditRoundedIcon from "@mui/icons-material/EditRounded";
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
import type { ItemStatus, ItemSummary, PagedResponse } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import type { AuthenticatedUser } from "../auth/types";
import { ItemFormDialog } from "./ItemFormDialog";

type ItemsScreenProps = {
  user: AuthenticatedUser;
};

const headCells = [
  { id: "name", label: "Name", minWidth: 240, sortable: true, align: undefined },
  { id: "code", label: "Code", minWidth: 140, sortable: true, align: undefined },
  { id: "unit", label: "Unit", minWidth: 120, sortable: false, align: undefined },
  { id: "price", label: "Price", minWidth: 120, sortable: true, align: "right" as const },
  { id: "taxRate", label: "Tax", minWidth: 120, sortable: false, align: "right" as const },
  { id: "status", label: "Status", minWidth: 120, sortable: true, align: undefined },
  { id: "actions", label: "Actions", minWidth: 180, sortable: false, align: "right" as const }
] as const;

const emptyPage: PagedResponse<ItemSummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "code" || orderBy === "status" || orderBy === "price") {
    return `${orderBy},${order}`;
  }

  return `name,${order}`;
}

function renderNullable(value: string | null): string {
  return value?.trim() ? value : "—";
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

function formatPercent(value: number): string {
  return new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value);
}

export function ItemsScreen({ user }: ItemsScreenProps) {
  const { getItems, deleteItem } = useAuth();
  const table = useTable({
    defaultOrderBy: "name",
    defaultOrder: "asc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | ItemStatus>("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<ItemSummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<ItemSummary | null>(null);

  const isCompanyAdmin = user.roles.includes("company-admin");
  const canCreateItem = isCompanyAdmin || user.roles.includes("user");
  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    void getItems({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
      status: status || undefined,
      search
    })
      .then((response) => {
        if (!active) {
          return;
        }

        setPageData(response);
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }

        setError(
          requestError instanceof ApiClientError
            ? requestError.message
            : "Unable to load items right now."
        );
        setPageData(emptyPage);
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [getItems, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage]);

  function canModifyItem(item: ItemSummary): boolean {
    if (isCompanyAdmin) {
      return true;
    }

    return item.createdByUserId === user.userId;
  }

  function handleSearchChange(event: React.ChangeEvent<HTMLInputElement>) {
    setSearch(event.target.value);
    table.onResetPage();
  }

  function handleStatusChange(event: SelectChangeEvent<"" | ItemStatus>) {
    setStatus(event.target.value as "" | ItemStatus);
    table.onResetPage();
  }

  function handleRefresh() {
    setReloadKey((current) => current + 1);
  }

  function handleCreate() {
    setSelectedItem(null);
    setIsDialogOpen(true);
  }

  function handleEdit(item: ItemSummary) {
    setSelectedItem(item);
    setIsDialogOpen(true);
  }

  async function handleDelete(item: ItemSummary) {
    if (!window.confirm(`Delete item "${item.name}"?`)) {
      return;
    }

    try {
      await deleteItem(item.id);
      handleRefresh();
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "Unable to delete item right now."
      );
    }
  }

  function handleDialogClose() {
    setIsDialogOpen(false);
    setSelectedItem(null);
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Items
        </Typography>
        <Typography variant="h3">Item catalog</Typography>
        <Typography color="text.secondary" variant="body1">
          Browse, search, and maintain items and services for the active company.
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
                onChange={handleSearchChange}
                placeholder="Search by name or code"
                value={search}
              />

              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="items-status-label">Status</InputLabel>
                <Select
                  label="Status"
                  labelId="items-status-label"
                  onChange={handleStatusChange}
                  value={status}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Stack>

            <Stack direction="row" spacing={1.5}>
              {canCreateItem ? (
                <Button onClick={handleCreate} startIcon={<AddRoundedIcon />} variant="contained">
                  New Item
                </Button>
              ) : null}
              <Button onClick={handleRefresh} startIcon={<RefreshRoundedIcon />} variant="outlined">
                Refresh
              </Button>
            </Stack>
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Typography color="text.secondary" variant="body2">
            {totalElements} {totalElements === 1 ? "item" : "items"} in this company
          </Typography>

          <Paper sx={{ overflow: "hidden" }} variant="outlined">
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headCells.map((cell) => (
                      <TableCell
                        align={cell.align}
                        key={cell.id}
                        sx={{ minWidth: cell.minWidth }}
                      >
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
                          {search || status
                            ? "No items match the current filters."
                            : "No items have been created yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((item) => (
                        <TableRow hover key={item.id}>
                          <TableCell>
                            <Stack spacing={0.5}>
                              <Typography fontWeight={600} variant="body2">
                                {item.name}
                              </Typography>
                              <Typography color="text.secondary" variant="caption">
                                {renderNullable(item.description)}
                              </Typography>
                            </Stack>
                          </TableCell>
                          <TableCell>{renderNullable(item.code)}</TableCell>
                          <TableCell>{item.unit}</TableCell>
                          <TableCell align="right">{formatMoney(item.price)}</TableCell>
                          <TableCell align="right">{formatPercent(item.taxRate)} %</TableCell>
                          <TableCell>
                            <Chip
                              color={item.status === "ACTIVE" ? "success" : "default"}
                              label={item.status === "ACTIVE" ? "Active" : "Inactive"}
                              size="small"
                              variant={item.status === "ACTIVE" ? "filled" : "outlined"}
                            />
                          </TableCell>
                          <TableCell align="right">
                            {canModifyItem(item) ? (
                              <Stack direction="row" justifyContent="flex-end" spacing={1}>
                                <Button
                                  onClick={() => handleEdit(item)}
                                  size="small"
                                  startIcon={<EditRoundedIcon />}
                                  variant="text"
                                >
                                  Edit
                                </Button>
                                <Button
                                  color="error"
                                  onClick={() => void handleDelete(item)}
                                  size="small"
                                  startIcon={<DeleteOutlineRoundedIcon />}
                                  variant="text"
                                >
                                  Delete
                                </Button>
                              </Stack>
                            ) : (
                              <Typography color="text.secondary" variant="body2">
                                View only
                              </Typography>
                            )}
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

      <ItemFormDialog
        item={selectedItem}
        onClose={handleDialogClose}
        onSaved={handleRefresh}
        open={isDialogOpen}
      />
    </Stack>
  );
}

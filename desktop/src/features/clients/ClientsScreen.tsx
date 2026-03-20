import { useEffect, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
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
import {
  emptyRows,
  useTable
} from "../../components/table";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { ClientStatus, ClientSummary, PagedResponse } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import type { AuthenticatedUser } from "../auth/types";
import { ClientFormDialog } from "./ClientFormDialog";

type ClientsScreenProps = {
  user: AuthenticatedUser;
};

const headCells = [
  { id: "name", label: "Name", minWidth: 220, sortable: true, align: undefined },
  { id: "pib", label: "PIB", minWidth: 120, sortable: false, align: undefined },
  { id: "mb", label: "MB", minWidth: 120, sortable: false, align: undefined },
  { id: "email", label: "Email", minWidth: 220, sortable: true, align: undefined },
  { id: "phone", label: "Phone", minWidth: 160, sortable: false, align: undefined },
  { id: "location", label: "Location", minWidth: 220, sortable: false, align: undefined },
  { id: "status", label: "Status", minWidth: 120, sortable: true, align: undefined },
  { id: "actions", label: "Actions", minWidth: 140, sortable: false, align: "right" as const }
] as const;

const emptyPage: PagedResponse<ClientSummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "email" || orderBy === "status") {
    return `${orderBy},${order}`;
  }

  return `name,${order}`;
}

function formatLocation(client: ClientSummary): string {
  return [client.address, client.city, client.country].filter(Boolean).join(", ");
}

function renderNullable(value: string | null): string {
  return value?.trim() ? value : "—";
}

export function ClientsScreen({ user }: ClientsScreenProps) {
  const { getClients } = useAuth();
  const table = useTable({
    defaultOrderBy: "name",
    defaultOrder: "asc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | ClientStatus>("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<ClientSummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedClient, setSelectedClient] = useState<ClientSummary | null>(null);

  const isCompanyAdmin = user.roles.includes("company-admin");
  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    void getClients({
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
            : "Unable to load clients right now."
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
  }, [getClients, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage]);

  function handleSearchChange(event: React.ChangeEvent<HTMLInputElement>) {
    setSearch(event.target.value);
    table.onResetPage();
  }

  function handleStatusChange(event: SelectChangeEvent<"" | ClientStatus>) {
    setStatus(event.target.value as "" | ClientStatus);
    table.onResetPage();
  }

  function handleRefresh() {
    setReloadKey((current) => current + 1);
  }

  function handleCreate() {
    setSelectedClient(null);
    setIsDialogOpen(true);
  }

  function handleEdit(client: ClientSummary) {
    setSelectedClient(client);
    setIsDialogOpen(true);
  }

  function handleDialogClose() {
    setIsDialogOpen(false);
    setSelectedClient(null);
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Clients
        </Typography>
        <Typography variant="h3">Client directory</Typography>
        <Typography color="text.secondary" variant="body1">
          Browse, search, and maintain clients for the active company.
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
                placeholder="Search by name, PIB, MB, email, phone, or address"
                value={search}
              />

              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="clients-status-label">Status</InputLabel>
                <Select
                  label="Status"
                  labelId="clients-status-label"
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
              {isCompanyAdmin ? (
                <Button onClick={handleCreate} startIcon={<AddRoundedIcon />} variant="contained">
                  New Client
                </Button>
              ) : null}
              <Button onClick={handleRefresh} startIcon={<RefreshRoundedIcon />} variant="outlined">
                Refresh
              </Button>
            </Stack>
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Typography color="text.secondary" variant="body2">
            {totalElements} {totalElements === 1 ? "client" : "clients"} in this company
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
                            ? "No clients match the current filters."
                            : "No clients have been created yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((client) => (
                        <TableRow hover key={client.id}>
                          <TableCell>
                            <Stack spacing={0.5}>
                              <Typography fontWeight={600} variant="body2">
                                {client.name}
                              </Typography>
                              <Typography color="text.secondary" variant="caption">
                                {renderNullable(client.address)}
                              </Typography>
                            </Stack>
                          </TableCell>
                          <TableCell>{renderNullable(client.pib)}</TableCell>
                          <TableCell>{renderNullable(client.mb)}</TableCell>
                          <TableCell>{renderNullable(client.email)}</TableCell>
                          <TableCell>{renderNullable(client.phone)}</TableCell>
                          <TableCell>{formatLocation(client) || "—"}</TableCell>
                          <TableCell>
                            <Chip
                              color={client.status === "ACTIVE" ? "success" : "default"}
                              label={client.status === "ACTIVE" ? "Active" : "Inactive"}
                              size="small"
                              variant={client.status === "ACTIVE" ? "filled" : "outlined"}
                            />
                          </TableCell>
                          <TableCell align="right">
                            {isCompanyAdmin ? (
                              <Button
                                onClick={() => handleEdit(client)}
                                size="small"
                                startIcon={<EditRoundedIcon />}
                                variant="text"
                              >
                                Edit
                              </Button>
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

      <ClientFormDialog
        client={selectedClient}
        onClose={handleDialogClose}
        onSaved={handleRefresh}
        open={isDialogOpen}
      />
    </Stack>
  );
}

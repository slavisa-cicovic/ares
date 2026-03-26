import { useEffect, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
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
import type { CompanySummary, CreatedPlatformUserResponse, PagedResponse, UserSummary } from "../../lib/api/types";
import { CustomDialog } from "../../components";
import { useAuth } from "../auth/AuthProvider";
import { SuperAdminUserFormDialog } from "./SuperAdminUserFormDialog";

const headCells = [
  { id: "email", label: "Email", minWidth: 220, sortable: true, align: undefined },
  { id: "name", label: "Name", minWidth: 200, sortable: false, align: undefined },
  { id: "companyName", label: "Company", minWidth: 220, sortable: false, align: undefined },
  { id: "roles", label: "Roles", minWidth: 180, sortable: false, align: undefined },
  { id: "status", label: "Status", minWidth: 120, sortable: true, align: undefined }
] as const;

const emptyUserPage: PagedResponse<UserSummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "status") {
    return `status,${order}`;
  }

  return `email,${order}`;
}

export function SuperAdminUsersScreen() {
  const { createAdminUser, getAdminCompanies, getAdminUsers } = useAuth();
  const table = useTable({
    defaultOrderBy: "email",
    defaultOrder: "asc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<UserSummary>>(emptyUserPage);
  const [companies, setCompanies] = useState<CompanySummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [createdUser, setCreatedUser] = useState<CreatedPlatformUserResponse | null>(null);

  const rows = pageData.items;
  const totalElements = pageData.totalElements;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);

  function blurActiveElement() {
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }

  useEffect(() => {
    let active = true;

    void getAdminCompanies({
      page: 0,
      size: 100,
      sort: "name,asc"
    })
      .then((response) => {
        if (active) {
          setCompanies(response.items);
        }
      })
      .catch(() => {
        if (active) {
          setCompanies([]);
        }
      });

    return () => {
      active = false;
    };
  }, [getAdminCompanies]);

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    void getAdminUsers({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
      status: status || undefined,
      search
    })
      .then((response) => {
        if (active) {
          setPageData(response);
        }
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }

        setError(
          requestError instanceof ApiClientError
            ? requestError.message
            : "Unable to load users right now."
        );
        setPageData(emptyUserPage);
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [getAdminUsers, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage]);

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Users
        </Typography>
        <Typography variant="h3">Platform users</Typography>
        <Typography color="text.secondary" variant="body1">
          Create the first company admin users and review accounts across all companies.
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
                label="Search"
                onChange={(event) => {
                  setSearch(event.target.value);
                  table.onResetPage();
                }}
                placeholder="Search by email, name, or company"
                sx={{ minWidth: 280, maxWidth: 360 }}
                value={search}
              />

              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="admin-users-status-label">Status</InputLabel>
                <Select
                  label="Status"
                  labelId="admin-users-status-label"
                  onChange={(event: SelectChangeEvent<"" | "ACTIVE" | "INACTIVE">) => {
                    setStatus(event.target.value as "" | "ACTIVE" | "INACTIVE");
                    table.onResetPage();
                  }}
                  value={status}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Stack>

            <Stack direction="row" spacing={1.5}>
              <Button
                disabled={companies.length === 0}
                onClick={() => {
                  blurActiveElement();
                  setIsDialogOpen(true);
                }}
                startIcon={<AddRoundedIcon />}
                variant="contained"
              >
                New User
              </Button>
              <Button onClick={() => setReloadKey((current) => current + 1)} startIcon={<RefreshRoundedIcon />} variant="outlined">
                Refresh
              </Button>
            </Stack>
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Paper sx={{ overflow: "hidden" }} variant="outlined">
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headCells.map((cell) => (
                      <TableCell key={cell.id} sx={{ minWidth: cell.minWidth }}>
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
                          {search || status ? "No users match the current filters." : "No users have been created yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((user) => (
                        <TableRow hover key={user.id}>
                          <TableCell>{user.email}</TableCell>
                          <TableCell>{`${user.firstName} ${user.lastName}`}</TableCell>
                          <TableCell>{user.companyName}</TableCell>
                          <TableCell>
                            <Stack direction="row" flexWrap="wrap" gap={0.75}>
                              {user.roles.map((role) => (
                                <Chip key={role} label={role} size="small" variant="outlined" />
                              ))}
                            </Stack>
                          </TableCell>
                          <TableCell>
                            <Chip
                              color={user.status === "ACTIVE" ? "success" : "default"}
                              label={user.status === "ACTIVE" ? "Active" : user.status}
                              size="small"
                              variant={user.status === "ACTIVE" ? "filled" : "outlined"}
                            />
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

      <SuperAdminUserFormDialog
        companies={companies}
        onClose={() => setIsDialogOpen(false)}
        onCreated={(user) => {
          setIsDialogOpen(false);
          setCreatedUser(user);
          setReloadKey((current) => current + 1);
          table.onResetPage();
        }}
        onSubmit={createAdminUser}
        open={isDialogOpen}
      />

      <CustomDialog
        actions={
          <Button onClick={() => setCreatedUser(null)} variant="contained">
            Close
          </Button>
        }
        description="This password is shown once. Deliver it to the user through your internal onboarding process."
        onClose={() => setCreatedUser(null)}
        open={Boolean(createdUser)}
        size="sm"
        title="Temporary Password"
      >
        {createdUser ? (
          <Stack spacing={2}>
            <Alert severity="success">
              {createdUser.firstName} {createdUser.lastName} was created for {createdUser.companyName}.
            </Alert>
            <Box>
              <Typography color="text.secondary" gutterBottom variant="body2">
                Temporary Password
              </Typography>
              <Paper sx={{ p: 2, typography: "h6" }} variant="outlined">
                {createdUser.temporaryPassword}
              </Paper>
            </Box>
          </Stack>
        ) : null}
      </CustomDialog>
    </Stack>
  );
}

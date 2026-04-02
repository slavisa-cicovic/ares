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
import { ConfirmDialog, CustomDialog } from "../../components";
import { emptyRows, useTable } from "../../components/table";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { CreatedUserResponse, PagedResponse, PasswordResetResponse, UserSummary } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { UserFormDialog } from "./UserFormDialog";

const headCells = [
  { id: "email", label: "Email", minWidth: 240, sortable: true },
  { id: "name", label: "Name", minWidth: 220, sortable: false },
  { id: "roles", label: "Roles", minWidth: 180, sortable: false },
  { id: "status", label: "Status", minWidth: 120, sortable: true },
  { id: "actions", label: "Actions", minWidth: 160, sortable: false }
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

export function UsersScreen() {
  const { createUser, getUsers, resetUserPassword } = useAuth();
  const table = useTable({
    defaultOrderBy: "email",
    defaultOrder: "asc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<UserSummary>>(emptyUserPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [createdUser, setCreatedUser] = useState<CreatedUserResponse | null>(null);
  const [resetTarget, setResetTarget] = useState<UserSummary | null>(null);
  const [isResetting, setIsResetting] = useState(false);
  const [resetResult, setResetResult] = useState<PasswordResetResponse | null>(null);

  const rows = pageData.items;
  const emptyRowsCount = emptyRows(table.page, table.rowsPerPage, rows.length);

  function blurActiveElement() {
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    void getUsers({
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
  }, [getUsers, reloadKey, search, status, table.order, table.orderBy, table.page, table.rowsPerPage]);

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Users
        </Typography>
        <Typography variant="h3">Company users</Typography>
        <Typography color="text.secondary" variant="body1">
          Manage users inside the active company workspace.
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
                placeholder="Search by email or name"
                sx={{ minWidth: 280, maxWidth: 360 }}
                value={search}
              />

              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="users-status-label">Status</InputLabel>
                <Select
                  label="Status"
                  labelId="users-status-label"
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
                    ? rows.map((row) => (
                        <TableRow hover key={row.id}>
                          <TableCell>{row.email}</TableCell>
                          <TableCell>{`${row.firstName} ${row.lastName}`}</TableCell>
                          <TableCell>
                            <Stack direction="row" flexWrap="wrap" gap={1}>
                              {row.roles.map((role) => (
                                <Chip key={role} label={role} size="small" variant="outlined" />
                              ))}
                            </Stack>
                          </TableCell>
                          <TableCell>{row.status}</TableCell>
                          <TableCell>
                            <Button
                              onClick={() => {
                                blurActiveElement();
                                setResetTarget(row);
                              }}
                              size="small"
                              variant="text"
                            >
                              Reset Password
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    : null}

                  {!isLoading && emptyRowsCount > 0
                    ? Array.from({ length: emptyRowsCount }).map((_, index) => (
                        <TableRow key={`empty-${index}`} sx={{ height: 69 }}>
                          <TableCell colSpan={headCells.length} />
                        </TableRow>
                      ))
                    : null}
                </TableBody>
              </Table>
            </TableContainer>

            <TablePagination
              component="div"
              count={pageData.totalElements}
              onPageChange={(_, nextPage) => table.onChangePage(nextPage)}
              onRowsPerPageChange={table.onChangeRowsPerPage}
              page={table.page}
              rowsPerPage={table.rowsPerPage}
              rowsPerPageOptions={[10, 25, 50]}
            />
          </Paper>
        </Stack>
      </Paper>

      <UserFormDialog
        onClose={() => setIsDialogOpen(false)}
        onCreated={(user) => {
          setIsDialogOpen(false);
          setCreatedUser(user);
          setReloadKey((current) => current + 1);
        }}
        onSubmit={createUser}
        open={isDialogOpen}
      />

      <CustomDialog
        actions={
          <Button
            onClick={() => setCreatedUser(null)}
            variant="contained"
          >
            Close
          </Button>
        }
        description="Copy this temporary password now. It will not be shown again."
        onClose={() => setCreatedUser(null)}
        open={createdUser !== null}
        title="Temporary Password"
      >
        <Stack spacing={2}>
          <Alert severity="success">
            User <strong>{createdUser?.email}</strong> was created successfully.
          </Alert>

          <Paper sx={{ p: 2 }} variant="outlined">
            <Stack spacing={0.5}>
              <Typography color="text.secondary" variant="body2">
                Temporary Password
              </Typography>
              <Typography sx={{ fontFamily: "monospace", fontSize: 18, fontWeight: 700 }} variant="h6">
                {createdUser?.temporaryPassword}
              </Typography>
            </Stack>
          </Paper>
        </Stack>
      </CustomDialog>

      <ConfirmDialog
        confirmLabel="Reset Password"
        description="Generate a new temporary password for this user."
        isLoading={isResetting}
        onClose={() => {
          if (!isResetting) {
            setResetTarget(null);
          }
        }}
        onConfirm={async () => {
          if (!resetTarget) {
            return;
          }

          setIsResetting(true);

          try {
            const response = await resetUserPassword(resetTarget.id);
            setResetTarget(null);
            setResetResult(response);
          } catch (requestError) {
            setError(
              requestError instanceof ApiClientError
                ? requestError.message
                : "Unable to reset password right now."
            );
            setResetTarget(null);
          } finally {
            setIsResetting(false);
          }
        }}
        open={Boolean(resetTarget)}
        title="Reset Password"
        content={
          <Typography color="text.secondary" variant="body2">
            Reset password for <strong>{resetTarget?.email}</strong>?
          </Typography>
        }
      />

      <CustomDialog
        actions={
          <Button onClick={() => setResetResult(null)} variant="contained">
            Close
          </Button>
        }
        description="Copy this temporary password now. It will not be shown again."
        onClose={() => setResetResult(null)}
        open={Boolean(resetResult)}
        title="Temporary Password"
      >
        {resetResult ? (
          <Stack spacing={2}>
            <Alert severity="success">
              Password was reset for <strong>{resetResult.email}</strong>.
            </Alert>

            <Paper sx={{ p: 2 }} variant="outlined">
              <Stack spacing={0.5}>
                <Typography color="text.secondary" variant="body2">
                  Temporary Password
                </Typography>
                <Typography sx={{ fontFamily: "monospace", fontSize: 18, fontWeight: 700 }} variant="h6">
                  {resetResult.temporaryPassword}
                </Typography>
              </Stack>
            </Paper>
          </Stack>
        ) : null}
      </CustomDialog>
    </Stack>
  );
}

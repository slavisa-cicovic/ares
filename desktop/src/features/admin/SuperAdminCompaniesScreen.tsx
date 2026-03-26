import { useEffect, useState } from "react";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import {
  Alert,
  Box,
  Button,
  Chip,
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
import { emptyRows, useTable } from "../../components/table";
import type { CompanySummary, PagedResponse } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "../auth/AuthProvider";
import { SuperAdminCompanyFormDialog } from "./SuperAdminCompanyFormDialog";

const headCells = [
  { id: "name", label: "Name", minWidth: 260, sortable: true, align: undefined },
  { id: "pib", label: "PIB", minWidth: 140, sortable: true, align: undefined },
  { id: "mb", label: "MB", minWidth: 140, sortable: true, align: undefined },
  { id: "status", label: "Status", minWidth: 120, sortable: true, align: undefined }
] as const;

const emptyPage: PagedResponse<CompanySummary> = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
};

function toSortValue(orderBy: string, order: "asc" | "desc"): string {
  if (orderBy === "pib" || orderBy === "mb" || orderBy === "status") {
    return `${orderBy},${order}`;
  }

  return `name,${order}`;
}

export function SuperAdminCompaniesScreen() {
  const { getAdminCompanies, createAdminCompany } = useAuth();
  const table = useTable({
    defaultOrderBy: "name",
    defaultOrder: "asc",
    defaultRowsPerPage: 10
  });
  const [search, setSearch] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [pageData, setPageData] = useState<PagedResponse<CompanySummary>>(emptyPage);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

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

    setIsLoading(true);
    setError(null);

    void getAdminCompanies({
      page: table.page,
      size: table.rowsPerPage,
      sort: toSortValue(table.orderBy, table.order),
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
            : "Unable to load companies right now."
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
  }, [getAdminCompanies, reloadKey, search, table.order, table.orderBy, table.page, table.rowsPerPage]);

  function handleRefresh() {
    setReloadKey((current) => current + 1);
  }

  function handleCreated() {
    setIsDialogOpen(false);
    setReloadKey((current) => current + 1);
    table.onResetPage();
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Companies
        </Typography>
        <Typography variant="h3">Platform companies</Typography>
        <Typography color="text.secondary" variant="body1">
          Create and browse tenant companies across the whole Ares installation.
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
            <TextField
              fullWidth
              label="Search"
              onChange={(event) => {
                setSearch(event.target.value);
                table.onResetPage();
              }}
              placeholder="Search by company name, PIB, or MB"
              sx={{ maxWidth: 360 }}
              value={search}
            />

            <Stack direction="row" spacing={1.5}>
              <Button
                onClick={() => {
                  blurActiveElement();
                  setIsDialogOpen(true);
                }}
                startIcon={<AddRoundedIcon />}
                variant="contained"
              >
                New Company
              </Button>
              <Button onClick={handleRefresh} startIcon={<RefreshRoundedIcon />} variant="outlined">
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
                          {search ? "No companies match the current search." : "No companies have been created yet."}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : null}

                  {!isLoading
                    ? rows.map((company) => (
                        <TableRow hover key={company.id}>
                          <TableCell>
                            <Typography fontWeight={600} variant="body2">
                              {company.name}
                            </Typography>
                          </TableCell>
                          <TableCell>{company.pib ?? "—"}</TableCell>
                          <TableCell>{company.mb ?? "—"}</TableCell>
                          <TableCell>
                            <Chip
                              color={company.status === "ACTIVE" ? "success" : "default"}
                              label={company.status === "ACTIVE" ? "Active" : company.status}
                              size="small"
                              variant={company.status === "ACTIVE" ? "filled" : "outlined"}
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

      <SuperAdminCompanyFormDialog
        onClose={() => setIsDialogOpen(false)}
        onCreated={handleCreated}
        onSubmit={createAdminCompany}
        open={isDialogOpen}
      />
    </Stack>
  );
}

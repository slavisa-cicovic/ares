import { useEffect, useState } from "react";
import {
  TableEmptyRows,
  TableHeadCustom,
  TableNoData,
  TablePaginationCustom,
  TableSkeleton,
  emptyRows,
  useTable,
  type TableHeadCell
} from "../../components/table";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { ClientStatus, ClientSummary, PagedResponse } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import type { AuthenticatedUser } from "../auth/types";
import { ClientFormDialog } from "./ClientFormDialog";

type ClientsScreenProps = {
  user: AuthenticatedUser;
};

const headCells: TableHeadCell[] = [
  { id: "name", label: "Name", minWidth: 220 },
  { id: "pib", label: "PIB", sortable: false, minWidth: 120 },
  { id: "mb", label: "MB", sortable: false, minWidth: 120 },
  { id: "email", label: "Email", minWidth: 220 },
  { id: "phone", label: "Phone", sortable: false, minWidth: 160 },
  { id: "location", label: "Location", sortable: false, minWidth: 220 },
  { id: "status", label: "Status", minWidth: 120 },
  { id: "actions", label: "Actions", sortable: false, align: "right", minWidth: 140 }
];

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

  function handleStatusChange(event: React.ChangeEvent<HTMLSelectElement>) {
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
    <div className="module-screen">
      <div className="screen-intro">
        <span className="eyebrow">Clients</span>
        <h3>Client directory</h3>
      </div>

      <div className="module-card">
        <div className="module-toolbar">
          <div className="module-toolbar-group module-toolbar-group-grow">
            <label className="field">
              <span>Search</span>
              <input
                onChange={handleSearchChange}
                placeholder="Search by name, PIB, MB, email, phone, or address"
                value={search}
              />
            </label>

            <label className="field module-filter-field">
              <span>Status</span>
              <select onChange={handleStatusChange} value={status}>
                <option value="">All</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
          </div>

          <div className="module-toolbar-group">
            {isCompanyAdmin ? (
              <button className="primary-button" onClick={handleCreate} type="button">
                New Client
              </button>
            ) : null}
            <button className="secondary-button" onClick={handleRefresh} type="button">
              Refresh
            </button>
            {!isCompanyAdmin ? <span className="muted-note">Read-only access.</span> : null}
          </div>
        </div>

        {error ? <div className="error-banner inline-banner">{error}</div> : null}

        <div className="module-meta-row">
          <span className="muted-note">
            {totalElements} {totalElements === 1 ? "client" : "clients"} in this company
          </span>
        </div>

        <div className="ui-table-wrapper">
          <table className="ui-table">
            <TableHeadCustom
              headLabel={headCells}
              onSort={table.onSort}
              order={table.order}
              orderBy={table.orderBy}
            />

            <tbody>
              {isLoading
                ? Array.from({ length: table.rowsPerPage }).map((_, index) => (
                    <TableSkeleton key={`skeleton-${index}`} colSpan={headCells.length} />
                  ))
                : null}

              {!isLoading && rows.length === 0 ? (
                <TableNoData
                  colSpan={headCells.length}
                  notFound
                  title={
                    search || status
                      ? "No clients match the current filters."
                      : "No clients have been created yet."
                  }
                />
              ) : null}

              {!isLoading
                ? rows.map((client) => (
                    <tr key={client.id}>
                      <td>
                        <div className="table-primary-cell">
                          <strong>{client.name}</strong>
                          <span>{renderNullable(client.address)}</span>
                        </div>
                      </td>
                      <td>{renderNullable(client.pib)}</td>
                      <td>{renderNullable(client.mb)}</td>
                      <td>{renderNullable(client.email)}</td>
                      <td>{renderNullable(client.phone)}</td>
                      <td>{formatLocation(client) || "—"}</td>
                      <td>
                        <span
                          className={`table-status-pill ${client.status === "ACTIVE" ? "active" : "inactive"}`}
                        >
                          {client.status === "ACTIVE" ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td className="table-actions-cell">
                        {isCompanyAdmin ? (
                          <button
                            className="secondary-button"
                            onClick={() => handleEdit(client)}
                            type="button"
                          >
                            Edit
                          </button>
                        ) : (
                          <span className="muted-note">View only</span>
                        )}
                      </td>
                    </tr>
                  ))
                : null}

              {!isLoading && emptyRowsCount > 0 ? (
                <TableEmptyRows
                  colSpan={headCells.length}
                  emptyRows={emptyRowsCount}
                  height={table.dense ? 52 : 66}
                />
              ) : null}
            </tbody>
          </table>
        </div>

        <TablePaginationCustom
          count={totalElements}
          dense={table.dense}
          denseVisible
          note=""
          onChangeDense={table.onChangeDense}
          onChangePage={table.onChangePage}
          onChangeRowsPerPage={table.onChangeRowsPerPage}
          page={table.page}
          rowsPerPage={table.rowsPerPage}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </div>

      <ClientFormDialog
        client={selectedClient}
        onClose={handleDialogClose}
        onSaved={handleRefresh}
        open={isDialogOpen}
      />
    </div>
  );
}

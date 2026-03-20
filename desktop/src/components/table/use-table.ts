import { useState } from "react";
import type { TableOrder, TableState, UseTableProps } from "./types";

export function useTable(props?: UseTableProps): TableState {
  const [dense, setDense] = useState(Boolean(props?.defaultDense));
  const [page, setPage] = useState(props?.defaultCurrentPage ?? 0);
  const [orderBy, setOrderBy] = useState(props?.defaultOrderBy ?? "name");
  const [rowsPerPage, setRowsPerPage] = useState(props?.defaultRowsPerPage ?? 10);
  const [order, setOrder] = useState<TableOrder>(props?.defaultOrder ?? "asc");
  const [selected, setSelected] = useState<string[]>(props?.defaultSelected ?? []);

  function onSort(id: string) {
    if (!id) {
      return;
    }

    const isAsc = orderBy === id && order === "asc";
    setOrder(isAsc ? "desc" : "asc");
    setOrderBy(id);
  }

  function onSelectRow(inputValue: string) {
    const newSelected = selected.includes(inputValue)
      ? selected.filter((value) => value !== inputValue)
      : [...selected, inputValue];

    setSelected(newSelected);
  }

  function onChangeRowsPerPage(event: React.ChangeEvent<HTMLSelectElement>) {
    setPage(0);
    setRowsPerPage(Number.parseInt(event.target.value, 10));
  }

  function onChangeDense(event: React.ChangeEvent<HTMLInputElement>) {
    setDense(event.target.checked);
  }

  function onSelectAllRows(checked: boolean, inputValue: string[]) {
    if (checked) {
      setSelected(inputValue);
      return;
    }

    setSelected([]);
  }

  function onChangePage(newPage: number) {
    setPage(newPage);
  }

  function onResetPage() {
    setPage(0);
  }

  function onUpdatePageDeleteRow(totalRowsInPage: number) {
    setSelected([]);

    if (page && totalRowsInPage < 2) {
      setPage(page - 1);
    }
  }

  function onUpdatePageDeleteRows(args: {
    totalRows: number;
    totalRowsInPage: number;
    totalRowsFiltered: number;
  }) {
    const totalSelected = selected.length;

    setSelected([]);

    if (!page) {
      return;
    }

    if (totalSelected === args.totalRowsInPage) {
      setPage(page - 1);
      return;
    }

    if (totalSelected === args.totalRowsFiltered) {
      setPage(0);
      return;
    }

    if (totalSelected > args.totalRowsInPage) {
      const newPage = Math.ceil((args.totalRows - totalSelected) / rowsPerPage) - 1;
      setPage(newPage);
    }
  }

  return {
    dense,
    page,
    rowsPerPage,
    order,
    orderBy,
    selected,
    onSelectRow,
    onSelectAllRows,
    onResetPage,
    onSort,
    onChangePage,
    onChangeRowsPerPage,
    onChangeDense,
    onUpdatePageDeleteRow,
    onUpdatePageDeleteRows,
    setPage,
    setDense,
    setOrder,
    setOrderBy,
    setSelected,
    setRowsPerPage
  };
}

export default useTable;

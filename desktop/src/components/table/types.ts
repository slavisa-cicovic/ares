import type { ChangeEvent, Dispatch, ReactNode, SetStateAction } from "react";

export type TableOrder = "asc" | "desc";

export type TableHeadCell = {
  id: string;
  label: ReactNode;
  align?: "left" | "center" | "right";
  width?: number | string;
  minWidth?: number | string;
  sortable?: boolean;
};

export type TableState = {
  dense: boolean;
  page: number;
  rowsPerPage: number;
  order: TableOrder;
  orderBy: string;
  selected: string[];
  onSelectRow: (id: string) => void;
  onSelectAllRows: (checked: boolean, rowIds: string[]) => void;
  onResetPage: VoidFunction;
  onSort: (id: string) => void;
  onChangePage: (newPage: number) => void;
  onChangeRowsPerPage: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onChangeDense: (event: ChangeEvent<HTMLInputElement>) => void;
  onUpdatePageDeleteRow: (totalRowsInPage: number) => void;
  onUpdatePageDeleteRows: (args: {
    totalRows: number;
    totalRowsInPage: number;
    totalRowsFiltered: number;
  }) => void;
  setPage: Dispatch<SetStateAction<number>>;
  setDense: Dispatch<SetStateAction<boolean>>;
  setOrder: Dispatch<SetStateAction<TableOrder>>;
  setOrderBy: Dispatch<SetStateAction<string>>;
  setSelected: Dispatch<SetStateAction<string[]>>;
  setRowsPerPage: Dispatch<SetStateAction<number>>;
};

export type UseTableProps = {
  defaultDense?: boolean;
  defaultOrder?: TableOrder;
  defaultOrderBy?: string;
  defaultSelected?: string[];
  defaultRowsPerPage?: number;
  defaultCurrentPage?: number;
};

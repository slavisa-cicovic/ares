import type { TableHeadCell, TableOrder } from "./types";

type TableHeadCustomProps = {
  order?: TableOrder;
  orderBy?: string;
  headLabel: TableHeadCell[];
  rowCount?: number;
  numSelected?: number;
  onSort?: (id: string) => void;
  onSelectAllRows?: (checked: boolean) => void;
};

export function TableHeadCustom({
  order = "asc",
  orderBy,
  headLabel,
  rowCount = 0,
  numSelected = 0,
  onSort,
  onSelectAllRows
}: TableHeadCustomProps) {
  const allChecked = rowCount > 0 && numSelected === rowCount;
  const partlyChecked = numSelected > 0 && numSelected < rowCount;

  return (
    <thead className="ui-table-head">
      <tr>
        {onSelectAllRows ? (
          <th className="ui-table-checkbox-cell">
            <input
              aria-label="Select all rows"
              checked={allChecked}
              onChange={(event) => onSelectAllRows(event.target.checked)}
              ref={(element) => {
                if (element) {
                  element.indeterminate = partlyChecked;
                }
              }}
              type="checkbox"
            />
          </th>
        ) : null}

        {headLabel.map((headCell) => {
          const isActive = orderBy === headCell.id;

          return (
            <th
              key={headCell.id}
              className={`ui-table-th ${headCell.sortable === false ? "" : "sortable"} ${isActive ? "active" : ""}`.trim()}
              style={{
                width: headCell.width,
                minWidth: headCell.minWidth,
                textAlign: headCell.align ?? "left"
              }}
            >
              {onSort && headCell.sortable !== false ? (
                <button
                  className="ui-table-sort-button"
                  onClick={() => onSort(headCell.id)}
                  type="button"
                >
                  <span>{headCell.label}</span>
                  <span aria-hidden="true" className="ui-table-sort-indicator">
                    {isActive ? (order === "asc" ? "↑" : "↓") : "↕"}
                  </span>
                  <span className="ui-visually-hidden">
                    {isActive ? `sorted ${order}` : "not sorted"}
                  </span>
                </button>
              ) : (
                <span>{headCell.label}</span>
              )}
            </th>
          );
        })}
      </tr>
    </thead>
  );
}

export default TableHeadCustom;

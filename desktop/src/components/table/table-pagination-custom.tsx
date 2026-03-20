type TablePaginationCustomProps = {
  count: number;
  page: number;
  rowsPerPage: number;
  rowsPerPageOptions?: number[];
  denseVisible?: boolean;
  dense?: boolean;
  onChangePage: (newPage: number) => void;
  onChangeRowsPerPage: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  onChangeDense?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  note?: string;
};

export function TablePaginationCustom({
  count,
  page,
  rowsPerPage,
  rowsPerPageOptions = [5, 10, 25, 50, 100],
  denseVisible,
  dense,
  onChangePage,
  onChangeRowsPerPage,
  onChangeDense,
  note
}: TablePaginationCustomProps) {
  const totalPages = Math.max(1, Math.ceil(count / rowsPerPage));
  const currentPage = count === 0 ? 0 : page + 1;

  return (
    <div className="ui-table-pagination">
      <div className="ui-table-pagination-note">
        <span>{note}</span>

        {denseVisible && onChangeDense ? (
          <label className="ui-table-dense-toggle">
            <input checked={Boolean(dense)} onChange={onChangeDense} type="checkbox" />
            <span>Dense</span>
          </label>
        ) : null}
      </div>

      <div className="ui-table-pagination-controls">
        <label className="ui-table-rows-per-page">
          <span>Rows</span>
          <select onChange={onChangeRowsPerPage} value={rowsPerPage}>
            {rowsPerPageOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>

        <span className="ui-table-page-indicator">
          {currentPage} / {totalPages}
        </span>

        <button
          className="secondary-button"
          disabled={page <= 0}
          onClick={() => onChangePage(page - 1)}
          type="button"
        >
          Previous
        </button>

        <button
          className="secondary-button"
          disabled={page + 1 >= totalPages}
          onClick={() => onChangePage(page + 1)}
          type="button"
        >
          Next
        </button>
      </div>
    </div>
  );
}

export default TablePaginationCustom;

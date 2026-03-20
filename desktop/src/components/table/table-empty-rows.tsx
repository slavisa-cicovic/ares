type TableEmptyRowsProps = {
  emptyRows: number;
  height?: number;
  colSpan?: number;
};

export function TableEmptyRows({
  emptyRows,
  height = 56,
  colSpan = 12
}: TableEmptyRowsProps) {
  if (!emptyRows) {
    return null;
  }

  return (
    <>
      {Array.from({ length: emptyRows }).map((_, index) => (
        <tr key={index} style={{ height }}>
          <td colSpan={colSpan} />
        </tr>
      ))}
    </>
  );
}

export default TableEmptyRows;

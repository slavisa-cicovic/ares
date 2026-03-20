type TableNoDataProps = {
  title?: string;
  notFound: boolean;
  colSpan?: number;
};

export function TableNoData({ title, notFound, colSpan = 12 }: TableNoDataProps) {
  if (!notFound) {
    return null;
  }

  return (
    <tr>
      <td className="ui-table-empty-cell" colSpan={colSpan}>
        {title ?? "No data"}
      </td>
    </tr>
  );
}

export default TableNoData;

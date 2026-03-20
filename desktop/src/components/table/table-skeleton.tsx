type TableSkeletonProps = {
  colSpan?: number;
};

export function TableSkeleton({ colSpan = 12 }: TableSkeletonProps) {
  return (
    <tr>
      <td className="ui-table-skeleton-cell" colSpan={colSpan}>
        <div className="ui-table-skeleton">
          <span />
          <span />
          <span />
          <span />
        </div>
      </td>
    </tr>
  );
}

export default TableSkeleton;

import type { TableOrder } from "./types";

export type Comparable = string | number | boolean | Date | null | undefined;

export function emptyRows(page: number, rowsPerPage: number, arrayLength: number) {
  void page;
  return Math.max(0, rowsPerPage - arrayLength);
}

function descendingComparator<T>(a: T, b: T, orderBy: keyof T) {
  const valueA = a[orderBy] as Comparable;
  const valueB = b[orderBy] as Comparable;

  if ((valueB === null || valueB === undefined) && (valueA === null || valueA === undefined)) {
    return 0;
  }

  if (valueA !== null && valueA !== undefined && (valueB === null || valueB === undefined)) {
    return 1;
  }

  if (valueB !== null && valueB !== undefined && (valueA === null || valueA === undefined)) {
    return -1;
  }

  if (typeof valueA === "string" && typeof valueB === "string") {
    const aLower = valueA.toLowerCase();
    const bLower = valueB.toLowerCase();

    if (aLower < bLower) {
      return -1;
    }

    if (aLower > bLower) {
      return 1;
    }

    return 0;
  }

  if (typeof valueA === "number" && typeof valueB === "number") {
    return valueB - valueA;
  }

  if (typeof valueA === "boolean" && typeof valueB === "boolean") {
    if (valueB === valueA) {
      return 0;
    }

    return valueA ? -1 : 1;
  }

  if (valueA instanceof Date && valueB instanceof Date) {
    return valueB.getTime() - valueA.getTime();
  }

  return 0;
}

export function getComparator<Key extends PropertyKey>(
  order: TableOrder,
  orderBy: Key
): (a: Record<Key, Comparable>, b: Record<Key, Comparable>) => number {
  return order === "desc"
    ? (a, b) => -descendingComparator(a, b, orderBy)
    : (a, b) => descendingComparator(a, b, orderBy);
}

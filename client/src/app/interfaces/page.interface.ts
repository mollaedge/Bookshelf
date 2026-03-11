export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElement: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
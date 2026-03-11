export interface Book {
  id: number;
  title: string;
  authorName: string;
  isbn: string;
  synopsis?: string;
  genre?: string;
  owner: string;
  cover?: string; // Base64 encoded image from byte[]
  coverUrl?: string; // URL to the cover image
  rate?: number;
  favourite?: boolean;
  archived?: boolean;
  shareable?: boolean;
  read?: boolean;
}

export interface RequestedBook {
  id: number;
  title: string;
  authorName: string;
  isbn: string;
  requesterName: string;
  rate?: number;
  requested?: boolean;
  requestApproved?: boolean;
  cover?: string; // Base64 encoded image from byte[]
  coverUrl?: string; // URL to the cover image
}

export enum BookSearchSource {
  GOOGLE_BOOKS = 'GOOGLE_BOOKS',
  OPEN_LIBRARY = 'OPEN_LIBRARY'
}

export interface BookSearchResultDto {
  title: string;
  authors: string[];
  isbn: string;
  publishedDate: string;
  synopsis: string;
  genre: string;
  pageCount: number;
  coverUrl: string;
  publisher?: string;
  externalId?: string;
  source: BookSearchSource;
  previewLink?: string;
}
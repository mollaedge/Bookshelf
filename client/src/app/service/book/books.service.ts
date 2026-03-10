import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElement: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

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

@Injectable({
  providedIn: 'root'
})
export class BooksService {
  private apiUrl = environment.apiUrl;
  private baseUrl = `${this.apiUrl}/books`;

  // Note: Authorization headers are automatically added by AuthInterceptor
  constructor(private http: HttpClient) { }

  // Get all shareable books
  getAllShareableBooks(page: number = 0, size: number = 15): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  // Get all user's books
  getMyBooks(page: number = 0, size: number = 15): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`${this.baseUrl}/owner?page=${page}&size=${size}`);
  }

  // Get returned books
  getReturnedBooks(page: number = 0, size: number = 15): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`${this.baseUrl}/returned?page=${page}&size=${size}`);
  }

  // Get borrowed books
  getBorrowedBooks(page: number = 0, size: number = 15): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`${this.baseUrl}/borrowed?page=${page}&size=${size}`);
  }

  // Get requested books
  getRequestedBooks(page: number = 0, size: number = 15): Observable<PageResponse<RequestedBook>> {
    return this.http.get<PageResponse<RequestedBook>>(`${this.baseUrl}/requested?page=${page}&size=${size}`);
  }

  // Approve a book request
  approveRequest(bookId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/approve-request/${bookId}`, {});
  }

  // Reject a book request
  rejectRequest(bookId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/reject-request/${bookId}`, {});
  }

  // Get a single book by ID
  getBookById(bookId: number): Observable<Book> {
    return this.http.get<Book>(`${this.baseUrl}/${bookId}`);
  }

  // Create a new book
  createBook(book: Partial<Book>): Observable<Book> {
    return this.http.post<Book>(this.baseUrl, book);
  }

  // Update a book
  updateBook(bookId: number, book: Partial<Book>): Observable<Book> {
    return this.http.put<Book>(`${this.baseUrl}/${bookId}`, book);
  }

  // Delete a book
  deleteBook(bookId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${bookId}`);
  }

  // Request a book
  requestBook(bookId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/borrow/${bookId}`, {});
  }

  // Return a borrowed book
  returnBook(bookId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/return/${bookId}`, {});
  }

  // Get recently added books from user's library
  getRecentBooks(size: number = 3): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`${this.baseUrl}/owner/recent?size=${size}`);
  }

  // Search books from external sources (Google Books, Open Library)
  searchBooks(query: string, source: BookSearchSource, page: number = 0, size: number = 15): Observable<PageResponse<BookSearchResultDto>> {
    return this.http.get<PageResponse<BookSearchResultDto>>(`${this.baseUrl}/search`, {
      params: { query, source, page: page.toString(), size: size.toString() }
    });
  }

  // Search books from Google Books API (deprecated - use searchBooks instead)
  searchExternalBooks(query: string, page: number = 0, pageSize: number = 10): Observable<any> {
    const startIndex = page * pageSize;
    const googleBooksUrl = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(query)}&startIndex=${startIndex}&maxResults=${pageSize}`;
    return this.http.get(googleBooksUrl);
  }

  // Upload book cover image
  uploadBookCover(bookId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.baseUrl}/${bookId}/cover`, formData);
  }
}

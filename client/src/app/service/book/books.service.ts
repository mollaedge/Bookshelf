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

  // Search books from Google Books API
  searchExternalBooks(query: string): Observable<any> {
    const googleBooksUrl = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(query)}&maxResults=10`;
    return this.http.get(googleBooksUrl);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Book {
  id: string;
  title: string;
  author: string;
  description?: string;
  image?: string;
  rentedDate?: Date;
  returnedDate?: Date;
  requestedDate?: Date;
  dueDate?: Date;
  renterName?: string;
  requesterName?: string;
  status?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BooksService {
  private baseUrl = 'http://localhost:8088/api/v1/books';

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('authToken');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // Get all user's books
  getMyBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.baseUrl, { headers: this.getAuthHeaders() });
  }

  // Get returned books
  getReturnedBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.baseUrl}/returned`, { headers: this.getAuthHeaders() });
  }

  // Get rented books
  getRentedBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.baseUrl}/rented`, { headers: this.getAuthHeaders() });
  }

  // Get requested books
  getRequestedBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.baseUrl}/requested`, { headers: this.getAuthHeaders() });
  }

  // Approve a book request
  approveRequest(bookId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/approve-request/${bookId}`, {}, { headers: this.getAuthHeaders() });
  }

  // Reject a book request
  rejectRequest(bookId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/reject-request/${bookId}`, {}, { headers: this.getAuthHeaders() });
  }

  // Get a single book by ID
  getBookById(bookId: string): Observable<Book> {
    return this.http.get<Book>(`${this.baseUrl}/${bookId}`, { headers: this.getAuthHeaders() });
  }

  // Create a new book
  createBook(book: Partial<Book>): Observable<Book> {
    return this.http.post<Book>(this.baseUrl, book, { headers: this.getAuthHeaders() });
  }

  // Update a book
  updateBook(bookId: string, book: Partial<Book>): Observable<Book> {
    return this.http.put<Book>(`${this.baseUrl}/${bookId}`, book, { headers: this.getAuthHeaders() });
  }

  // Delete a book
  deleteBook(bookId: string): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${bookId}`, { headers: this.getAuthHeaders() });
  }

  // Search books from Google Books API
  searchExternalBooks(query: string): Observable<any> {
    const googleBooksUrl = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(query)}&maxResults=10`;
    return this.http.get(googleBooksUrl);
  }
}

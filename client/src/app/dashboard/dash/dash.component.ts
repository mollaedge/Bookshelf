import { Component, OnInit } from '@angular/core';
import { BooksService, Book, PageResponse } from '../../service/book/books.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-dash',
  standalone: false,
  templateUrl: './dash.component.html',
  styleUrl: './dash.component.scss'
})
export class DashComponent implements OnInit {
  recommendedBooks: Book[] = [];
  totalBooks: number = 0;
  loading: boolean = false;
  error: string = '';

  constructor(
    private booksService: BooksService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRecommendedBooks();
  }

  loadRecommendedBooks(): void {
    this.loading = true;
    this.error = '';

    // Get all books with pagination (first page, larger size to show more)
    this.booksService.getAllShareableBooks(0, 50).subscribe({
      next: (response: PageResponse<Book>) => {
        this.recommendedBooks = response.content;
        this.totalBooks = response.totalElement;
        this.loading = false;
      },
      error: (err) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.router.navigate(['/auth']);
          return;
        }
        console.log(err.message);
        this.error = 'Failed to load books.';
        this.loading = false;
        console.error('Error loading books:', err);
      }
    });
  }

  requestBook(bookId: number): void {
    this.booksService.requestBook(bookId).subscribe({
      next: () => {
        // Optionally show success message or refresh the list
        alert('Book requested successfully!');
      },
      error: (err) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.router.navigate(['/auth']);
          return;
        }
        console.error('Error requesting book:', err);
        alert('Failed to request book. Please try again.');
      }
    });
  }
}

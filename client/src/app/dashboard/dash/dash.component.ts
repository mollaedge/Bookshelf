import { Component, OnInit } from '@angular/core';
import { BooksService, Book, PageResponse } from '../../service/book/books.service';
import { ProfileService } from '../../service/profile/profile.service';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { UserDashboardResponse } from '../../interfaces/user.interface';
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

  userDashboard: UserDashboardResponse | null = null;
  dashboardLoading: boolean = false;
  isLoggedIn: boolean = false;

  recentBooks: Book[] = [];
  recentBooksLoading: boolean = false;

  constructor(
    private booksService: BooksService,
    private profileService: ProfileService,
    private authState: AuthStateService
  ) {}

  ngOnInit(): void {
    this.loadRecommendedBooks();

    this.isLoggedIn = !!this.authState.getCurrentUser();
    if (this.isLoggedIn) {
      this.loadUserDashboard();
      this.loadRecentBooks();
    }
  }

  loadUserDashboard(): void {
    this.dashboardLoading = true;
    this.profileService.getDashboard().subscribe({
      next: (data) => {
        this.userDashboard = data;
        this.dashboardLoading = false;
      },
      error: () => {
        this.dashboardLoading = false;
      }
    });
  }

  loadRecentBooks(): void {
    this.recentBooksLoading = true;
    this.booksService.getRecentBooks(3).subscribe({
      next: (response) => {
        this.recentBooks = response.content;
        this.recentBooksLoading = false;
      },
      error: () => {
        this.recentBooksLoading = false;
      }
    });
  }

  loadRecommendedBooks(): void {
    this.loading = true;
    this.error = '';

    this.booksService.getAllShareableBooks(0, 50).subscribe({
      next: (response: PageResponse<Book>) => {
        this.recommendedBooks = response.content;
        this.totalBooks = response.totalElement;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err instanceof HttpErrorResponse && err.status === 0) {
          this.error = 'Service is temporarily unavailable. Please try again later.';
        } else {
          this.error = 'Failed to load books. Please try again.';
        }
      }
    });
  }

  requestBook(bookId: number): void {
    this.booksService.requestBook(bookId).subscribe({
      next: () => {
        alert('Book requested successfully!');
      },
      error: (err) => {
        if (err instanceof HttpErrorResponse && err.status === 0) {
          alert('Service is temporarily unavailable. Please try again later.');
        } else {
          alert('Failed to request book. Please try again.');
        }
      }
    });
  }

  get maxStatValue(): number {
    if (!this.userDashboard) return 1;
    const { booksOwned, booksRead, currentlyBorrowed } = this.userDashboard.stats;
    return Math.max(booksOwned, booksRead, currentlyBorrowed, 1);
  }

  readBarWidth(value: number): number {
    return Math.round((value / this.maxStatValue) * 100);
  }
}

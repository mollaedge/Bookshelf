import { Component, OnInit } from '@angular/core';
import { BooksService, Book, RequestedBook, PageResponse } from '../../service/book/books.service';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-mybooks',
  standalone: false,
  templateUrl: './mybooks.component.html',
  styleUrl: './mybooks.component.scss'
})
export class MybooksComponent implements OnInit {
  activeTab: 'mybooks' | 'returned' | 'borrowed' | 'requested' = 'mybooks';
  
  myBooks: Book[] = [];
  returnedBooks: Book[] = [];
  borrowedBooks: Book[] = [];
  requestedBooks: RequestedBook[] = [];
  
  // Pagination properties
  currentPage: number = 0;
  pageSize: number = 15;
  totalPages: number = 0;
  totalElements: number = 0;
  isFirstPage: boolean = true;
  isLastPage: boolean = true;
  
  loading: boolean = false;
  error: string = '';
  showAddBookModal: boolean = false;

  constructor(
    private booksService: BooksService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: 'mybooks' | 'returned' | 'borrowed' | 'requested'): void {
    this.activeTab = tab;
    this.currentPage = 0; // Reset to first page when switching tabs
    this.loadData();
  }

  loadData(page: number = 0): void {
    this.loading = true;
    this.error = '';
    this.currentPage = page;

    const serviceCall = this.getServiceCall(page);
    
    serviceCall.subscribe({
      next: (response: PageResponse<Book | RequestedBook>) => {
        const filteredContent = this.filterContentByTab(response.content);
        this.updateActiveTabData(filteredContent);
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElement;
        this.isFirstPage = response.first;
        this.isLastPage = response.last;
        this.loading = false;
      },
      error: (err: any) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.router.navigate(['/auth']);
          return;
        }
        console.log(err.message);
        this.error = 'Failed to load data. Please try again.';
        this.loading = false;
        console.error('Error loading data:', err);
      }
    });
  }

  private getServiceCall(page: number): Observable<PageResponse<Book | RequestedBook>> {
    switch (this.activeTab) {
      case 'mybooks':
        return this.booksService.getMyBooks(page, this.pageSize);
      case 'returned':
        return this.booksService.getReturnedBooks(page, this.pageSize);
      case 'borrowed':
        return this.booksService.getBorrowedBooks(page, this.pageSize);
      case 'requested':
        return this.booksService.getRequestedBooks(page, this.pageSize);
      default:
        return this.booksService.getMyBooks(page, this.pageSize);
    }
  }

  private filterContentByTab(content: (Book | RequestedBook)[]): Book[] | RequestedBook[] {
    if (this.activeTab === 'requested') {
      return content as RequestedBook[];
    }
    return content as Book[];
  }

  private updateActiveTabData(data: Book[] | RequestedBook[]): void {
    switch (this.activeTab) {
      case 'mybooks':
        this.myBooks = data as Book[];
        break;
      case 'returned':
        this.returnedBooks = data as Book[];
        break;
      case 'borrowed':
        this.borrowedBooks = data as Book[];
        break;
      case 'requested':
        this.requestedBooks = data as RequestedBook[];
        break;
    }
  }

  isOverdue(dueDate: Date | undefined): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.loadData(page);
    }
  }

  nextPage(): void {
    if (!this.isLastPage) {
      this.loadData(this.currentPage + 1);
    }
  }

  previousPage(): void {
    if (!this.isFirstPage) {
      this.loadData(this.currentPage - 1);
    }
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  approveRequest(bookId: number): void {
    this.booksService.approveRequest(bookId).subscribe({
      next: () => {
        this.loadData(this.currentPage);
      },
      error: (err: any) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.router.navigate(['/auth']);
          return;
        }
        this.error = 'Failed to approve request.';
        console.error('Error approving request:', err);
      }
    });
  }

  rejectRequest(bookId: number): void {
    this.booksService.rejectRequest(bookId).subscribe({
      next: () => {
        this.loadData(this.currentPage);
      },
      error: (err: any) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.router.navigate(['/auth']);
          return;
        }
        this.error = 'Failed to reject request.';
        console.error('Error rejecting request:', err);
      }
    });
  }

  deleteBook(bookId: number): void {
    Swal.fire({
      title: 'Are you sure?',
      text: "You won't be able to revert this!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Yes, delete it!',
      cancelButtonText: 'Cancel'
    }).then((result) => {
      if (result.isConfirmed) {
        this.booksService.deleteBook(bookId).subscribe({
          next: () => {
            Swal.fire({
              title: 'Deleted!',
              text: 'Your book has been deleted.',
              icon: 'success',
              timer: 2000,
              showConfirmButton: false
            });
            this.loadData(this.currentPage);
          },
          error: (err: any) => {
            if (err instanceof HttpErrorResponse && err.status === 403) {
              this.router.navigate(['/auth']);
              return;
            }
            this.error = 'Failed to delete book.';
            Swal.fire({
              title: 'Error!',
              text: 'Failed to delete the book. Please try again.',
              icon: 'error',
              confirmButtonText: 'OK'
            });
            console.error('Error deleting book:', err);
          }
        });
      }
    });
  }

  openAddBookModal(): void {
    this.showAddBookModal = true;
  }

  closeAddBookModal(): void {
    this.showAddBookModal = false;
  }

  handleBookSaved(book: Book): void {
    this.loadData(); // Reload the list after book is saved
  }
}

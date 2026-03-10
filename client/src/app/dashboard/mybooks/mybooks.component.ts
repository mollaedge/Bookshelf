import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import Swal from 'sweetalert2';
import { BooksService, Book, RequestedBook, PageResponse } from '../../service/book/books.service';

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

  currentPage = 0;
  pageSize = 15;
  totalPages = 0;
  totalElements = 0;
  isFirstPage = true;
  isLastPage = true;
  loading = false;
  error = '';

  showPopup = false;
  popupMode: 'add' | 'edit' = 'add';
  selectedBook: Book | null = null;

  constructor(private booksService: BooksService) {}

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: 'mybooks' | 'returned' | 'borrowed' | 'requested'): void {
    this.activeTab = tab;
    this.currentPage = 0;
    this.loadData();
  }

  loadData(page: number = 0): void {
    this.loading = true;
    this.error = '';
    this.currentPage = page;

    this.getServiceCall(page).subscribe({
      next: (response: PageResponse<Book | RequestedBook>) => {
        this.setTabData(response.content);
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElement;
        this.isFirstPage = response.first;
        this.isLastPage = response.last;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load books. Please try again.';
        this.loading = false;
      }
    });
  }

  private getServiceCall(page: number): Observable<PageResponse<Book | RequestedBook>> {
    switch (this.activeTab) {
      case 'mybooks':   return this.booksService.getMyBooks(page, this.pageSize);
      case 'returned':  return this.booksService.getReturnedBooks(page, this.pageSize);
      case 'borrowed':  return this.booksService.getBorrowedBooks(page, this.pageSize);
      case 'requested': return this.booksService.getRequestedBooks(page, this.pageSize);
      default:          return this.booksService.getMyBooks(page, this.pageSize);
    }
  }

  private setTabData(content: (Book | RequestedBook)[]): void {
    switch (this.activeTab) {
      case 'mybooks':   this.myBooks = content as Book[]; break;
      case 'returned':  this.returnedBooks = content as Book[]; break;
      case 'borrowed':  this.borrowedBooks = content as Book[]; break;
      case 'requested': this.requestedBooks = content as RequestedBook[]; break;
    }
  }

  nextPage(): void {
    if (!this.isLastPage) this.loadData(this.currentPage + 1);
  }

  prevPage(): void {
    if (!this.isFirstPage) this.loadData(this.currentPage - 1);
  }

  approveRequest(bookId: number): void {
    this.booksService.approveRequest(bookId).subscribe({
      next: () => this.loadData(this.currentPage),
      error: () => { this.error = 'Failed to approve request. Please try again.'; }
    });
  }

  rejectRequest(bookId: number): void {
    this.booksService.rejectRequest(bookId).subscribe({
      next: () => this.loadData(this.currentPage),
      error: () => { this.error = 'Failed to reject request. Please try again.'; }
    });
  }

  returnBook(bookId: number): void {
    Swal.fire({
      title: 'Return Book?',
      text: 'Are you sure you want to return this book?',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, return it!',
      cancelButtonText: 'Cancel'
    }).then((result) => {
      if (result.isConfirmed) {
        this.booksService.returnBook(bookId).subscribe({
          next: () => {
            this.loadData(this.currentPage);
            Swal.fire('Returned!', 'The book has been returned successfully.', 'success');
          },
          error: () => {
            Swal.fire('Error', 'Failed to return the book. Please try again.', 'error');
          }
        });
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
            Swal.fire({ title: 'Deleted!', text: 'Your book has been deleted.', icon: 'success', timer: 2000, showConfirmButton: false });
            this.loadData(this.currentPage);
          },
          error: () => {
            Swal.fire({ title: 'Error!', text: 'Failed to delete the book. Please try again.', icon: 'error', confirmButtonText: 'OK' });
          }
        });
      }
    });
  }

  archiveBook(bookId: number): void {
    const book = this.myBooks.find(b => b.id === bookId);
    if (!book) return;
    this.booksService.updateBook(bookId, { archived: !book.archived }).subscribe({
      next: () => this.loadData(this.currentPage),
      error: () => { this.error = 'Failed to archive book. Please try again.'; }
    });
  }

  openAdd(): void {
    this.popupMode = 'add';
    this.selectedBook = null;
    this.showPopup = true;
  }

  openEdit(book: Book): void {
    this.popupMode = 'edit';
    this.selectedBook = book;
    this.showPopup = true;
  }

  closePopup(): void { this.showPopup = false; }

  onBookSaved(_book: Book): void { this.loadData(this.currentPage); }
}

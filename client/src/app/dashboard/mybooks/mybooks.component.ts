import { Component, OnInit } from '@angular/core';
import { BooksService, Book } from '../../service/book/books.service';

@Component({
  selector: 'app-mybooks',
  standalone: false,
  templateUrl: './mybooks.component.html',
  styleUrl: './mybooks.component.scss'
})
export class MybooksComponent implements OnInit {
  activeTab: 'mybooks' | 'returned' | 'rented' | 'requested' = 'mybooks';
  
  myBooks: Book[] = [];
  returnedBooks: Book[] = [];
  rentedBooks: Book[] = [];
  requestedBooks: Book[] = [];
  
  loading: boolean = false;
  error: string = '';
  showAddBookModal: boolean = false;

  constructor(private booksService: BooksService) {}

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: 'mybooks' | 'returned' | 'rented' | 'requested'): void {
    this.activeTab = tab;
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    const serviceCall = this.getServiceCall();
    
    serviceCall.subscribe({
      next: (data) => {
        this.updateActiveTabData(data);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load data. Please try again.';
        this.loading = false;
        console.error('Error loading data:', err);
      }
    });
  }

  private getServiceCall() {
    switch (this.activeTab) {
      case 'mybooks':
        return this.booksService.getMyBooks();
      case 'returned':
        return this.booksService.getReturnedBooks();
      case 'rented':
        return this.booksService.getRentedBooks();
      case 'requested':
        return this.booksService.getRequestedBooks();
      default:
        return this.booksService.getMyBooks();
    }
  }

  private updateActiveTabData(data: Book[]): void {
    switch (this.activeTab) {
      case 'mybooks':
        this.myBooks = data;
        break;
      case 'returned':
        this.returnedBooks = data;
        break;
      case 'rented':
        this.rentedBooks = data;
        break;
      case 'requested':
        this.requestedBooks = data;
        break;
    }
  }

  isOverdue(dueDate: Date | undefined): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  approveRequest(bookId: string): void {
    this.booksService.approveRequest(bookId).subscribe({
      next: () => {
        this.loadData();
      },
      error: (err) => {
        this.error = 'Failed to approve request.';
        console.error('Error approving request:', err);
      }
    });
  }

  rejectRequest(bookId: string): void {
    this.booksService.rejectRequest(bookId).subscribe({
      next: () => {
        this.loadData();
      },
      error: (err) => {
        this.error = 'Failed to reject request.';
        console.error('Error rejecting request:', err);
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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BooksService, Book } from '../../../service/book/books.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

interface ExternalBook {
  id: string;
  volumeInfo: {
    title: string;
    authors?: string[];
    description?: string;
    imageLinks?: {
      thumbnail?: string;
      smallThumbnail?: string;
    };
    publishedDate?: string;
    publisher?: string;
    pageCount?: number;
    categories?: string[];
  };
}

@Component({
  selector: 'app-bookpopup',
  standalone: false,
  templateUrl: './bookpopup.component.html',
  styleUrl: './bookpopup.component.scss'
})
export class BookpopupComponent implements OnInit {
  @Input() isOpen: boolean = false;
  @Input() bookToEdit?: Book;
  @Input() mode: 'add' | 'edit' = 'add';
  @Output() closePopup = new EventEmitter<void>();
  @Output() bookSaved = new EventEmitter<Book>();

  bookFormData: Partial<Book> = {};
  searchQuery: string = '';
  searchResults: ExternalBook[] = [];
  isSearching: boolean = false;
  showSearchResults: boolean = false;
  submitting: boolean = false;
  error: string = '';
  
  private searchSubject = new Subject<string>();

  constructor(private booksService: BooksService) {}

  ngOnInit(): void {
    // Set up search with debounce
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(query => {
      if (query.trim().length > 2) {
        this.searchExternalBooks(query);
      } else {
        this.searchResults = [];
        this.showSearchResults = false;
      }
    });

    // Initialize form with edit data if in edit mode
    if (this.mode === 'edit' && this.bookToEdit) {
      this.bookFormData = { ...this.bookToEdit };
    }
  }

  onSearchInput(query: string): void {
    this.searchQuery = query;
    this.searchSubject.next(query);
  }

  searchExternalBooks(query: string): void {
    this.isSearching = true;
    this.booksService.searchExternalBooks(query).subscribe({
      next: (response) => {
        this.searchResults = response.items || [];
        this.showSearchResults = true;
        this.isSearching = false;
      },
      error: (err) => {
        console.error('Error searching books:', err);
        this.isSearching = false;
        this.searchResults = [];
      }
    });
  }

  selectExternalBook(externalBook: ExternalBook): void {
    const volumeInfo = externalBook.volumeInfo;
    
    this.bookFormData = {
      title: volumeInfo.title,
      author: volumeInfo.authors?.join(', ') || 'Unknown Author',
      description: volumeInfo.description || '',
      image: volumeInfo.imageLinks?.thumbnail?.replace('http://', 'https://') || 
             volumeInfo.imageLinks?.smallThumbnail?.replace('http://', 'https://') || ''
    };

    this.showSearchResults = false;
    this.searchQuery = '';
    this.searchResults = [];
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.showSearchResults = false;
  }

  onSubmit(): void {
    if (!this.bookFormData.title || !this.bookFormData.author) {
      this.error = 'Title and Author are required';
      return;
    }

    this.submitting = true;
    this.error = '';

    const operation = this.mode === 'edit' && this.bookToEdit?.id
      ? this.booksService.updateBook(this.bookToEdit.id, this.bookFormData)
      : this.booksService.createBook(this.bookFormData);

    operation.subscribe({
      next: (book) => {
        this.submitting = false;
        this.bookSaved.emit(book);
        this.close();
      },
      error: (err) => {
        this.submitting = false;
        this.error = `Failed to ${this.mode} book. Please try again.`;
        console.error(`Error ${this.mode}ing book:`, err);
      }
    });
  }

  close(): void {
    this.bookFormData = {};
    this.searchQuery = '';
    this.searchResults = [];
    this.showSearchResults = false;
    this.error = '';
    this.closePopup.emit();
  }

  get modalTitle(): string {
    return this.mode === 'edit' ? 'Edit Book' : 'Add New Book';
  }

  get submitButtonText(): string {
    if (this.submitting) {
      return this.mode === 'edit' ? 'Updating...' : 'Adding...';
    }
    return this.mode === 'edit' ? 'Update Book' : 'Add Book';
  }
}

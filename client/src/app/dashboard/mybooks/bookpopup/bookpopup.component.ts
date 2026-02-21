import { Component, EventEmitter, Input, OnInit, Output, ViewChild, ChangeDetectorRef } from '@angular/core';
import { BooksService, Book } from '../../../service/book/books.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { NgForm } from '@angular/forms';

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
    industryIdentifiers?: { type: string; identifier: string }[];
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
  @ViewChild('formRef') formRef?: NgForm;

  bookFormData: Partial<Book> = {};
  searchQuery: string = '';
  searchResults: ExternalBook[] = [];
  isSearching: boolean = false;
  showSearchResults: boolean = false;
  error: string = '';
  isCoverUrlFromApi: boolean = false;

  currentPage: number = 0;
  readonly pageSize: number = 10;
  totalItems: number = 0;

  get totalPages(): number {
    return Math.ceil(this.totalItems / this.pageSize);
  }

  get firstResultIndex(): number {
    return this.totalItems === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get lastResultIndex(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalItems);
  }

  private searchSubject = new Subject<string>();

  constructor(
    private booksService: BooksService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Set up search with debounce — always resets to page 0 on new query
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(query => {
      if (query.trim().length > 2) {
        this.searchExternalBooks(query, 0);
      } else {
        this.searchResults = [];
        this.showSearchResults = false;
        this.totalItems = 0;
        this.currentPage = 0;
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

  searchExternalBooks(query: string, page: number): void {
    this.isSearching = true;
    this.currentPage = page;
    this.booksService.searchExternalBooks(query, page, this.pageSize).subscribe({
      next: (response) => {
        this.searchResults = response.items || [];
        this.totalItems = response.totalItems || 0;
        this.showSearchResults = true;
        this.isSearching = false;
      },
      error: () => {
        this.isSearching = false;
        this.searchResults = [];
        this.totalItems = 0;
      }
    });
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.searchExternalBooks(this.searchQuery, this.currentPage + 1);
    }
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.searchExternalBooks(this.searchQuery, this.currentPage - 1);
    }
  }

  selectExternalBook(externalBook: ExternalBook): void {
    const volumeInfo = externalBook.volumeInfo;

    const coverUrl = volumeInfo.imageLinks?.thumbnail?.replace('http://', 'https://') ||
             volumeInfo.imageLinks?.smallThumbnail?.replace('http://', 'https://') || '';

    this.bookFormData = {
      title: volumeInfo.title,
      authorName: volumeInfo.authors?.join(', ') || 'Unknown Author',
      synopsis: volumeInfo.description || '',
      genre: volumeInfo.categories?.join(', ') || '',
      coverUrl: coverUrl,
      isbn: volumeInfo.industryIdentifiers ? volumeInfo.industryIdentifiers[0].identifier : '',
    };

    this.isCoverUrlFromApi = !!coverUrl;

    this.showSearchResults = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.currentPage = 0;
    this.totalItems = 0;

    this.cdr.detectChanges();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.showSearchResults = false;
    this.currentPage = 0;
    this.totalItems = 0;
  }

  onSubmit(): void {
    if (!this.bookFormData.title || !this.bookFormData.authorName) {
      this.error = 'Title and Author are required';
      return;
    }

    this.error = '';

    const operation = this.mode === 'edit' && this.bookToEdit?.id
      ? this.booksService.updateBook(this.bookToEdit.id, this.bookFormData)
      : this.booksService.createBook(this.bookFormData);

    operation.subscribe({
      next: (book) => {
        this.bookSaved.emit(book);
        this.close();
      },
      error: () => {
        this.error = `Failed to ${this.mode} book. Please try again.`;
      }
    });
  }

  close(): void {
    this.bookFormData = {};
    this.searchQuery = '';
    this.searchResults = [];
    this.showSearchResults = false;
    this.error = '';
    this.isCoverUrlFromApi = false;
    this.currentPage = 0;
    this.totalItems = 0;
    this.closePopup.emit();
  }

  get modalTitle(): string {
    return this.mode === 'edit' ? 'Edit Book' : 'Add New Book';
  }

  get submitButtonText(): string {
    return this.mode === 'edit' ? 'Update Book' : 'Add Book';
  }
}

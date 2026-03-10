import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BooksService, Book, BookSearchResultDto, BookSearchSource } from '../../../service/book/books.service';

@Component({
  selector: 'app-bookpopup',
  standalone: false,
  templateUrl: './bookpopup.component.html',
  styleUrl: './bookpopup.component.scss'
})
export class BookpopupComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() mode: 'add' | 'edit' = 'add';
  @Input() book: Book | null = null;
  @Output() closePopup = new EventEmitter<void>();
  @Output() bookSaved = new EventEmitter<Book>();

  form: FormGroup;
  isSubmitting = false;
  error = '';
  
  // File upload properties
  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;
  isUploadingCover = false;

  // Search functionality
  searchQuery = '';
  searchSource: BookSearchSource = BookSearchSource.GOOGLE_BOOKS;
  searchResults: BookSearchResultDto[] = [];
  isSearching = false;
  searchError = '';
  showSearch = false;
  BookSearchSource = BookSearchSource; // Expose enum to template

  constructor(private fb: FormBuilder, private booksService: BooksService) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      authorName: ['', Validators.required],
      isbn: [''],
      synopsis: [''],
      genre: [''],
      coverUrl: [''],
      shareable: [false],
      favourite: [false],
      archived: [false],
      read: [false]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.error = '';
      this.isSubmitting = false;
      if (this.mode === 'edit' && this.book) {
        this.form.patchValue({
          title: this.book.title,
          authorName: this.book.authorName,
          isbn: this.book.isbn,
          synopsis: this.book.synopsis ?? '',
          genre: this.book.genre ?? '',
          coverUrl: this.book.coverUrl ?? '',
          shareable: this.book.shareable ?? false,
          favourite: this.book.favourite ?? false,
          archived: this.book.archived ?? false,
          read: this.book.read ?? false
        });
      } else {
        this.form.reset({ shareable: false, favourite: false, archived: false, read: false });
      }
    }
  }

  get popupTitle(): string {
    return this.mode === 'edit' ? 'Edit Book' : 'Add New Book';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.error = '';

    const value = this.form.value;
    const call$ = this.mode === 'edit' && this.book
      ? this.booksService.updateBook(this.book.id, value)
      : this.booksService.createBook(value);

    call$.subscribe({
      next: (saved) => {
        // If there's a selected file, upload it after saving the book
        if (this.selectedCoverFile && saved.id) {
          this.uploadCoverImage(saved.id, saved);
        } else {
          this.isSubmitting = false;
          this.bookSaved.emit(saved);
          this.closePopup.emit();
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.error = 'Failed to save book. Please try again.';
      }
    });
  }

  close(): void {
    if (!this.isSubmitting) {
      this.closePopup.emit();
      this.resetSearch();
      this.resetFileSelection();
    }
  }

  private resetFileSelection(): void {
    this.selectedCoverFile = null;
    this.coverPreviewUrl = null;
    this.isUploadingCover = false;
    // Reset the file input
    const fileInput = document.getElementById('coverFileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.resetSearch();
    }
  }

  resetSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.searchError = '';
    this.showSearch = false;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.error = 'Please select an image file.';
        return;
      }
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.error = 'File size should not exceed 5MB.';
        return;
      }
      
      this.selectedCoverFile = file;
      this.error = '';
      
      // Create preview URL
      const reader = new FileReader();
      reader.onload = (e) => {
        this.coverPreviewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeCoverImage(): void {
    this.selectedCoverFile = null;
    this.coverPreviewUrl = null;
    // Reset the file input
    const fileInput = document.getElementById('coverFileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }

  private uploadCoverImage(bookId: number, savedBook: Book): void {
    if (!this.selectedCoverFile) {
      this.isSubmitting = false;
      this.bookSaved.emit(savedBook);
      this.closePopup.emit();
      return;
    }

    this.isUploadingCover = true;
    this.booksService.uploadBookCover(bookId, this.selectedCoverFile).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.isUploadingCover = false;
        // Clear the file selection and preview after successful upload
        this.resetFileSelection();
        // Refresh the book to get updated cover
        this.booksService.getBookById(bookId).subscribe({
          next: (updatedBook) => {
            this.bookSaved.emit(updatedBook);
            this.closePopup.emit();
          },
          error: () => {
            this.bookSaved.emit(savedBook);
            this.closePopup.emit();
          }
        });
      },
      error: () => {
        this.isSubmitting = false;
        this.isUploadingCover = false;
        // Clear the file selection even on error to prevent confusion
        this.resetFileSelection();
        this.error = 'Book saved but failed to upload cover image.';
        // Still emit success since book was saved
        this.bookSaved.emit(savedBook);
      }
    });
  }

  performSearch(): void {
    if (!this.searchQuery.trim()) {
      this.searchError = 'Please enter a search query';
      return;
    }

    this.isSearching = true;
    this.searchError = '';
    this.searchResults = [];

    this.booksService.searchBooks(this.searchQuery, this.searchSource, 0, 15).subscribe({
      next: (response) => {
        this.searchResults = response.content;
        this.isSearching = false;
        if (this.searchResults.length === 0) {
          this.searchError = 'No results found';
        }
      },
      error: () => {
        this.isSearching = false;
        this.searchError = 'Search failed. Please try again.';
      }
    });
  }

  selectSearchResult(result: BookSearchResultDto): void {
    const authorName = result.authors && result.authors.length > 0 
      ? result.authors.join(', ') 
      : '';

    this.form.patchValue({
      title: result.title || '',
      authorName: authorName,
      isbn: result.isbn || '',
      synopsis: result.synopsis || '',
      genre: result.genre || '',
      coverUrl: result.coverUrl || ''
    });

    this.resetSearch();
  }
}

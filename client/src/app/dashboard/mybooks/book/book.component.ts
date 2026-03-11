import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Book } from '../../../interfaces/book.interface';

@Component({
  selector: 'app-book',
  standalone: false,
  templateUrl: './book.component.html',
  styleUrl: './book.component.scss'
})
export class BookComponent {
  @Input() book!: Book;
  @Output() editBook = new EventEmitter<Book>();
  @Output() deleteBook = new EventEmitter<number>();
  @Output() archiveBook = new EventEmitter<number>();

  onCardClick(): void {
    this.editBook.emit(this.book);
  }

  onEdit(event: Event): void {
    event.stopPropagation();
    this.editBook.emit(this.book);
  }

  onArchive(event: Event): void {
    event.stopPropagation();
    this.archiveBook.emit(this.book.id);
  }

  onDelete(event: Event): void {
    event.stopPropagation();
    this.deleteBook.emit(this.book.id);
  }
}

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-start-stream',
  standalone: false,
  templateUrl: './start-stream.component.html',
  styleUrls: ['./start-stream.component.scss']
})
export class StartStreamComponent {
  @Input() isOpen: boolean = false;
  @Input() streamError: string = '';
  @Output() closeModal = new EventEmitter<void>();
  @Output() streamStart = new EventEmitter<string>();

  streamTitle: string = '';

  onClose(): void {
    this.streamTitle = '';
    this.closeModal.emit();
  }

  onStartStream(): void {
    if (this.streamTitle.trim()) {
      this.streamStart.emit(this.streamTitle.trim());
      this.streamTitle = '';
    }
  }

  stopPropagation(event: Event): void {
    event.stopPropagation();
  }
}

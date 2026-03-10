import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { HomePostService, CreatePostRequest, UpdatePostRequest, HomePost } from '../../../service/home/home-post.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-create-post',
  standalone: false,
  templateUrl: './create-post.component.html',
  styleUrl: './create-post.component.scss'
})
export class CreatePostComponent implements OnChanges {
  @Input() isOpen: boolean = false;
  @Input() postToEdit: HomePost | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() postCreated = new EventEmitter<void>();

  newPostTitle: string = '';
  newPostContent: string = '';
  selectedFiles: File[] = [];
  creatingPost: boolean = false;
  error: string = '';

  constructor(private homePostService: HomePostService) {}

  ngOnChanges(changes: SimpleChanges): void {
    // Handle any changes - if modal is open and we have a post to edit, populate the form
    if (this.isOpen && this.postToEdit) {
      this.newPostTitle = this.postToEdit.title;
      this.newPostContent = this.postToEdit.content;
      this.selectedFiles = [];
      this.error = '';
    } 
    // If modal is open but no post to edit (creating new), reset form
    else if (this.isOpen && !this.postToEdit) {
      this.resetForm();
    }
    // If modal closes, reset
    else if (!this.isOpen) {
      this.resetForm();
    }
  }

  close(): void {
    this.closeModal.emit();
    this.resetForm();
  }

  resetForm(): void {
    this.newPostTitle = '';
    this.newPostContent = '';
    this.selectedFiles = [];
    this.error = '';
  }

  onFilesSelected(event: any): void {
    const files = event.target.files;
    if (files) {
      this.selectedFiles = Array.from(files);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  createPost(): void {
    if (!this.newPostTitle.trim() || !this.newPostContent.trim()) {
      this.error = 'Title and content are required';
      return;
    }

    this.creatingPost = true;
    this.error = '';

    // Check if we're editing or creating
    if (this.postToEdit) {
      // Update existing post
      const updateRequest: UpdatePostRequest = {
        title: this.newPostTitle.trim(),
        content: this.newPostContent.trim()
      };

      this.homePostService.updatePost(this.postToEdit.id, updateRequest).subscribe({
        next: (updatedPost) => {
          console.log('Post updated successfully:', updatedPost);
          this.creatingPost = false;
          this.postCreated.emit();
          this.close();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error updating post:', error);
          this.error = 'Failed to update post. Please try again.';
          this.creatingPost = false;
        }
      });
    } else {
      // Create new post
      const request: CreatePostRequest = {
        title: this.newPostTitle.trim(),
        content: this.newPostContent.trim()
      };

      this.homePostService.createPost(request, this.selectedFiles.length > 0 ? this.selectedFiles : undefined).subscribe({
        next: (postId) => {
          console.log('Post created with ID:', postId);
          this.creatingPost = false;
          this.postCreated.emit();
          this.close();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error creating post:', error);
          this.error = 'Failed to create post. Please try again.';
          this.creatingPost = false;
        }
      });
    }
  }

  onOverlayClick(event: MouseEvent): void {
    // Close modal when clicking on the overlay (not the content)
    if (event.target === event.currentTarget) {
      this.close();
    }
  }
}

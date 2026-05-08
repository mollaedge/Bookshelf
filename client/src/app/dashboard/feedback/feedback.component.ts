import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AppFeedbackDto, AppFeedbackRequest, PublicFeedbackDto } from '../../interfaces/feedback.interface';
import { FeedbackService } from '../../service/feedback/feedback.service';
import { PageResponse } from '../../interfaces/page.interface';
import { AuthStateService } from '../../service/auth/auth-state.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-feedback',
  standalone: false,
  templateUrl: './feedback.component.html',
  styleUrl: './feedback.component.scss'
})
export class FeedbackComponent implements OnInit {
  feedbacks: AppFeedbackDto[] = [];
  sortedFeedbacks: AppFeedbackDto[] = [];
  isLoggedIn = false;

  loading = false;
  error = '';
  activeTab: 'all' | 'my' = 'all';
  sortBy = 'newest';

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  isFirstPage = true;
  isLastPage = true;

  // Add modal
  showAddModal = false;
  submitting = false;
  submitError = '';
  newFeedback: AppFeedbackRequest = { title: '', description: '' };

  // Edit modal
  showEditModal = false;
  editingFeedback: AppFeedbackDto | null = null;
  editForm: AppFeedbackRequest = { title: '', description: '' };
  editError = '';

  // Comment modal
  showCommentModal = false;
  selectedFeedback: AppFeedbackDto | null = null;
  newComment = '';
  commentSubmitting = false;

  highlightedFeedbackId: number | null = null;

  constructor(
    private feedbackService: FeedbackService,
    private authService: AuthStateService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    let isInitialLoad = true;
    this.authService.user$.subscribe(user => { 
      const wasLoggedIn = this.isLoggedIn;
      this.isLoggedIn = !!user;
      
      // Reload feedbacks when auth state changes (skip initial load)
      if (!isInitialLoad && wasLoggedIn !== this.isLoggedIn) {
        this.loadFeedbacks(0);
      }
      isInitialLoad = false;
    });
    this.loadFeedbacks();

    // Handle feedbackId from notification navigation
    this.route.queryParams.subscribe(params => {
      const feedbackId = params['feedbackId'] ? +params['feedbackId'] : null;
      if (feedbackId) {
        this.highlightedFeedbackId = feedbackId;
        setTimeout(() => this.scrollToFeedback(feedbackId), 800);
      }
    });
  }

  loadFeedbacks(page: number = 0): void {
    this.loading = true;
    this.error = '';
    this.currentPage = page;

    // Use public API if user is not logged in
    if (!this.isLoggedIn) {
      this.feedbackService.getPublic(page, this.pageSize).subscribe({
        next: (response: PageResponse<PublicFeedbackDto>) => {
          this.feedbacks = response.content.map(this.mapPublicToAppFeedback);
          this.totalPages = response.totalPages;
          this.isFirstPage = response.first;
          this.isLastPage = response.last;
          this.applySort();
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load feedback. Please try again.';
          this.loading = false;
        }
      });
      return;
    }

    const call = this.activeTab === 'all'
      ? this.feedbackService.getAll(page, this.pageSize)
      : this.feedbackService.getMy(page, this.pageSize);

    call.subscribe({
      next: (response: PageResponse<AppFeedbackDto>) => {
        this.feedbacks = response.content;
        this.totalPages = response.totalPages;
        this.isFirstPage = response.first;
        this.isLastPage = response.last;
        this.applySort();
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load feedback. Please try again.';
        this.loading = false;
      }
    });
  }

  switchTab(tab: 'all' | 'my'): void {
    if (this.activeTab === tab) return;
    this.activeTab = tab;
    this.loadFeedbacks(0);
  }

  applySort(): void {
    const copy = [...this.feedbacks];
    switch (this.sortBy) {
      case 'most-upvoted':
        this.sortedFeedbacks = copy.sort((a, b) => b.upvoteCount - a.upvoteCount);
        break;
      case 'most-comments':
        this.sortedFeedbacks = copy.sort((a, b) => b.comments.length - a.comments.length);
        break;
      default:
        this.sortedFeedbacks = copy;
    }
  }

  onSortChange(sort: string): void {
    this.sortBy = sort;
    this.applySort();
  }

  nextPage(): void {
    if (!this.isLastPage) this.loadFeedbacks(this.currentPage + 1);
  }

  previousPage(): void {
    if (!this.isFirstPage) this.loadFeedbacks(this.currentPage - 1);
  }

  // --- Add Feedback ---

  openAddModal(): void {
    this.showAddModal = true;
    this.newFeedback = { title: '', description: '' };
    this.submitError = '';
  }

  closeAddModal(): void {
    this.showAddModal = false;
    this.submitError = '';
  }

  submitFeedback(): void {
    if (!this.newFeedback.title.trim() || !this.newFeedback.description.trim()) {
      this.submitError = 'Please fill in all fields.';
      return;
    }
    this.submitting = true;
    this.submitError = '';

    this.feedbackService.submit(this.newFeedback).subscribe({
      next: () => {
        this.submitting = false;
        this.closeAddModal();
        this.loadFeedbacks(0);
      },
      error: () => {
        this.submitting = false;
        this.submitError = 'Failed to submit feedback. Please try again.';
      }
    });
  }

  // --- Upvote ---

  toggleUpvote(feedback: AppFeedbackDto): void {
    // Optimistic update
    const wasUpvoted = feedback.upvotedByCurrentUser;
    feedback.upvotedByCurrentUser = !wasUpvoted;
    feedback.upvoteCount += wasUpvoted ? -1 : 1;

    this.feedbackService.toggleUpvote(feedback.id).subscribe({
      error: () => {
        // Revert on failure
        feedback.upvotedByCurrentUser = wasUpvoted;
        feedback.upvoteCount += wasUpvoted ? 1 : -1;
      }
    });
  }

  // --- Comments ---

  openComments(feedback: AppFeedbackDto): void {
    this.selectedFeedback = feedback;
    this.showCommentModal = true;
    this.newComment = '';
    this.commentSubmitting = false;
  }

  closeCommentModal(): void {
    this.showCommentModal = false;
    this.selectedFeedback = null;
    this.newComment = '';
  }

  submitComment(): void {
    if (!this.newComment.trim() || !this.selectedFeedback) return;
    this.commentSubmitting = true;
    const id = this.selectedFeedback.id;

    this.feedbackService.addComment(id, this.newComment).subscribe({
      next: () => {
        this.commentSubmitting = false;
        this.closeCommentModal();
        this.loadFeedbacks(this.currentPage);
      },
      error: () => {
        this.commentSubmitting = false;
      }
    });
  }

  // --- Edit ---

  openEditModal(feedback: AppFeedbackDto): void {
    this.editingFeedback = feedback;
    this.editForm = { title: feedback.title, description: feedback.description };
    this.editError = '';
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingFeedback = null;
    this.editError = '';
  }

  saveEdit(): void {
    if (!this.editForm.title.trim() || !this.editForm.description.trim()) {
      this.editError = 'Please fill in all fields.';
      return;
    }
    if (!this.editingFeedback) return;

    this.submitting = true;
    this.editError = '';

    this.feedbackService.edit(this.editingFeedback.id, this.editForm).subscribe({
      next: () => {
        this.submitting = false;
        this.closeEditModal();
        this.loadFeedbacks(this.currentPage);
      },
      error: () => {
        this.submitting = false;
        this.editError = 'Failed to save changes. Please try again.';
      }
    });
  }

  // --- Delete ---

  deleteFeedback(feedback: AppFeedbackDto): void {
    Swal.fire({
      title: 'Delete Feedback?',
      text: 'This action cannot be undone.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Yes, delete it!'
    }).then((result) => {
      if (result.isConfirmed) {
        this.feedbackService.delete(feedback.id).subscribe({
          next: () => {
            this.loadFeedbacks(this.currentPage);
          },
          error: () => {
            Swal.fire('Error', 'Failed to delete feedback. Please try again.', 'error');
          }
        });
      }
    });
  }

  // --- Helpers ---

  private scrollToFeedback(feedbackId: number): void {
    const el = document.getElementById(`feedback-${feedbackId}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'NEW': '#2196F3',
      'IN_PROGRESS': '#FF9800',
      'RESOLVED': '#4CAF50',
      'CLOSED': '#9E9E9E'
    };
    return colors[status] || '#9E9E9E';
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'NEW': 'New',
      'IN_PROGRESS': 'In Progress',
      'RESOLVED': 'Resolved',
      'CLOSED': 'Closed'
    };
    return labels[status] || status;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;
    if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
    return date.toLocaleDateString();
  }

  private mapPublicToAppFeedback(publicFeedback: PublicFeedbackDto): AppFeedbackDto {
    return {
      id: publicFeedback.id,
      title: publicFeedback.title,
      description: publicFeedback.description,
      status: publicFeedback.status,
      upvoteCount: publicFeedback.upvoteCount,
      upvotedByCurrentUser: false,
      ownFeedback: false,
      age: publicFeedback.age,
      author: publicFeedback.authorName,
      comments: publicFeedback.comments.map(comment => ({
        authorId: 0, // Not available in public API
        fullName: comment.authorName,
        message: comment.message,
        createdAt: comment.createdAt
      }))
    };
  }
}

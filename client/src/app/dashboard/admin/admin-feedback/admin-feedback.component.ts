import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../service/admin/admin.service';
import {
  AdminFeedbackResponse,
  AdminFeedbackRequest,
  AdminFeedbackStatus,
} from '../../../interfaces/admin.interface';
import { PageResponse } from '../../../interfaces/page.interface';

type StatusFilter = 'ALL' | AdminFeedbackStatus;

const STATUS_LABELS: Record<AdminFeedbackStatus, string> = {
  NEW: 'New',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

@Component({
  selector: 'app-admin-feedback',
  templateUrl: './admin-feedback.component.html',
  styleUrls: ['./admin-feedback.component.scss'],
  standalone: false
})
export class AdminFeedbackComponent implements OnInit {

  readonly statusFilters: { id: StatusFilter; label: string }[] = [
    { id: 'ALL',         label: 'All' },
    { id: 'NEW',         label: 'New' },
    { id: 'IN_PROGRESS', label: 'In Progress' },
    { id: 'RESOLVED',    label: 'Resolved' },
    { id: 'CLOSED',      label: 'Closed' },
  ];

  readonly allStatuses: AdminFeedbackStatus[] = ['NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  activeFilter: StatusFilter = 'ALL';
  page: PageResponse<AdminFeedbackResponse> | null = null;
  loading = false;
  error = '';
  currentPage = 0;
  readonly pageSize = 20;

  // Detail panel
  selected: AdminFeedbackResponse | null = null;

  // Edit modal
  editModalOpen = false;
  editForm: AdminFeedbackRequest = { title: '', description: '', status: 'NEW' };
  editSaving = false;
  editError = '';
  isCreating = false;

  // Delete confirm
  deleteTarget: AdminFeedbackResponse | null = null;
  deleteInProgress = false;

  // Comment
  newComment = '';
  commentSaving = false;
  commentError = '';
  deletingCommentIndex: number | null = null;

  // Status change in-flight
  statusChangingId: number | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadFeedbacks();
  }

  // ── Loading ──────────────────────────────────────────────────────────────

  loadFeedbacks(pg = this.currentPage): void {
    this.loading = true;
    this.error = '';
    const call = this.activeFilter === 'ALL'
      ? this.adminService.getFeedbacks(pg, this.pageSize)
      : this.adminService.getFeedbacksByStatus(this.activeFilter, pg, this.pageSize);

    call.subscribe({
      next: (res) => { this.page = res; this.currentPage = pg; this.loading = false; },
      error: () => { this.error = 'Failed to load feedback.'; this.loading = false; }
    });
  }

  setFilter(f: StatusFilter): void {
    this.activeFilter = f;
    this.currentPage = 0;
    this.selected = null;
    this.loadFeedbacks(0);
  }

  goToPage(pg: number): void {
    if (!this.page) return;
    if (pg < 0 || pg >= this.page.totalPages) return;
    this.loadFeedbacks(pg);
  }

  get pageNumbers(): number[] {
    if (!this.page) return [];
    const total = this.page.totalPages;
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) {
      range.push(i);
    }
    return range;
  }

  // ── Status quick-change ──────────────────────────────────────────────────

  changeStatus(fb: AdminFeedbackResponse, status: AdminFeedbackStatus): void {
    if (fb.status === status) return;
    this.statusChangingId = fb.id;
    this.adminService.updateFeedback(fb.id, { title: fb.title, description: fb.description, status }).subscribe({
      next: (updated) => {
        this.replaceFeedback(updated);
        if (this.selected?.id === updated.id) this.selected = updated;
        this.statusChangingId = null;
      },
      error: () => { this.statusChangingId = null; }
    });
  }

  // ── Detail panel ─────────────────────────────────────────────────────────

  selectFeedback(fb: AdminFeedbackResponse): void {
    this.selected = fb;
    this.newComment = '';
    this.commentError = '';
  }

  closeDetail(): void {
    this.selected = null;
  }

  // ── Create / Edit modal ──────────────────────────────────────────────────

  openCreate(): void {
    this.isCreating = true;
    this.editForm = { title: '', description: '', status: 'NEW' };
    this.editError = '';
    this.editModalOpen = true;
  }

  openEdit(fb: AdminFeedbackResponse): void {
    this.isCreating = false;
    this.editForm = { title: fb.title, description: fb.description, status: fb.status };
    this.editError = '';
    this.editModalOpen = true;
  }

  saveEdit(): void {
    this.editSaving = true;
    this.editError = '';
    const call = this.isCreating
      ? this.adminService.createFeedback(this.editForm)
      : this.adminService.updateFeedback(this.selected!.id, this.editForm);

    call.subscribe({
      next: (result) => {
        if (this.isCreating) {
          this.page = this.page
            ? { ...this.page, content: [result, ...this.page.content], totalElement: this.page.totalElement + 1 }
            : null;
        } else {
          this.replaceFeedback(result);
          if (this.selected?.id === result.id) this.selected = result;
        }
        this.editSaving = false;
        this.editModalOpen = false;
      },
      error: () => { this.editError = 'Failed to save.'; this.editSaving = false; }
    });
  }

  closeEdit(): void {
    this.editModalOpen = false;
  }

  // ── Delete ───────────────────────────────────────────────────────────────

  openDelete(fb: AdminFeedbackResponse): void {
    this.deleteTarget = fb;
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.deleteInProgress = true;
    this.adminService.deleteFeedback(this.deleteTarget.id).subscribe({
      next: () => {
        if (this.page) {
          this.page = {
            ...this.page,
            content: this.page.content.filter(f => f.id !== this.deleteTarget!.id),
            totalElement: this.page.totalElement - 1
          };
        }
        if (this.selected?.id === this.deleteTarget!.id) this.selected = null;
        this.deleteTarget = null;
        this.deleteInProgress = false;
      },
      error: () => { this.deleteInProgress = false; }
    });
  }

  cancelDelete(): void {
    this.deleteTarget = null;
  }

  // ── Comments ─────────────────────────────────────────────────────────────

  submitComment(): void {
    if (!this.selected || !this.newComment.trim()) return;
    this.commentSaving = true;
    this.commentError = '';
    this.adminService.addFeedbackComment(this.selected.id, this.newComment.trim()).subscribe({
      next: (updated) => {
        this.replaceFeedback(updated);
        this.selected = updated;
        this.newComment = '';
        this.commentSaving = false;
      },
      error: () => { this.commentError = 'Failed to add comment.'; this.commentSaving = false; }
    });
  }

  deleteComment(index: number): void {
    if (!this.selected) return;
    this.deletingCommentIndex = index;
    this.adminService.deleteFeedbackComment(this.selected.id, index).subscribe({
      next: (updated) => {
        this.replaceFeedback(updated);
        this.selected = updated;
        this.deletingCommentIndex = null;
      },
      error: () => { this.deletingCommentIndex = null; }
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private replaceFeedback(updated: AdminFeedbackResponse): void {
    if (this.page) {
      this.page = {
        ...this.page,
        content: this.page.content.map(f => f.id === updated.id ? updated : f)
      };
    }
  }

  statusLabel(s: AdminFeedbackStatus): string {
    return STATUS_LABELS[s] ?? s;
  }

  statusClass(s: AdminFeedbackStatus): string {
    const map: Record<AdminFeedbackStatus, string> = {
      NEW: 'status--new',
      IN_PROGRESS: 'status--progress',
      RESOLVED: 'status--resolved',
      CLOSED: 'status--closed',
    };
    return map[s] ?? '';
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  commentAuthor(c: any): string {
    return c.fullName || c.authorName || 'System';
  }
}


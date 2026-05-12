import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../service/admin/admin.service';
import { AdminUserResponse, AdminUpdateUserRequest } from '../../../interfaces/admin.interface';
import { PageResponse } from '../../../interfaces/page.interface';

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss'],
  standalone: false
})
export class AdminUsersComponent implements OnInit {
  usersPage: PageResponse<AdminUserResponse> | null = null;
  loading = false;
  error = '';
  currentPage = 0;
  readonly pageSize = 20;

  // Edit modal
  editModalOpen = false;
  editTarget: AdminUserResponse | null = null;
  editForm: AdminUpdateUserRequest = {};
  editSaving = false;
  editError = '';

  // Delete confirm
  deleteTarget: AdminUserResponse | null = null;
  deleteConfirmOpen = false;
  deleteInProgress = false;

  // Inline action feedback
  actionUserId: number | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(page = this.currentPage): void {
    this.loading = true;
    this.error = '';
    this.adminService.getUsers(page, this.pageSize).subscribe({
      next: (res) => {
        this.usersPage = res;
        this.currentPage = page;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users.';
        this.loading = false;
      }
    });
  }

  goToPage(page: number): void {
    if (!this.usersPage) return;
    if (page < 0 || page >= this.usersPage.totalPages) return;
    this.loadUsers(page);
  }

  get pageNumbers(): number[] {
    if (!this.usersPage) return [];
    const total = this.usersPage.totalPages;
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) {
      range.push(i);
    }
    return range;
  }

  // ── Toggle lock ──────────────────────────────────────────────────────────
  toggleLock(user: AdminUserResponse): void {
    this.actionUserId = user.id;
    const call = user.accountLocked
      ? this.adminService.unlockUser(user.id)
      : this.adminService.lockUser(user.id);
    call.subscribe({
      next: (updated) => this.replaceUser(updated),
      error: () => { this.actionUserId = null; }
    });
  }

  // ── Toggle enabled ───────────────────────────────────────────────────────
  toggleEnabled(user: AdminUserResponse): void {
    this.actionUserId = user.id;
    const call = user.enabled
      ? this.adminService.deactivateUser(user.id)
      : this.adminService.activateUser(user.id);
    call.subscribe({
      next: (updated) => this.replaceUser(updated),
      error: () => { this.actionUserId = null; }
    });
  }

  // ── Edit modal ───────────────────────────────────────────────────────────
  openEdit(user: AdminUserResponse): void {
    this.editTarget = user;
    this.editForm = {
      firstname: user.firstname,
      lastname: user.lastname,
      email: user.email,
      bio: user.bio ?? '',
      location: user.location ?? '',
    };
    this.editError = '';
    this.editModalOpen = true;
  }

  saveEdit(): void {
    if (!this.editTarget) return;
    this.editSaving = true;
    this.editError = '';
    this.adminService.updateUser(this.editTarget.id, this.editForm).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.editSaving = false;
        this.editModalOpen = false;
      },
      error: () => {
        this.editError = 'Failed to save changes.';
        this.editSaving = false;
      }
    });
  }

  closeEdit(): void {
    this.editModalOpen = false;
    this.editTarget = null;
  }

  // ── Delete ───────────────────────────────────────────────────────────────
  openDelete(user: AdminUserResponse): void {
    this.deleteTarget = user;
    this.deleteConfirmOpen = true;
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.deleteInProgress = true;
    this.adminService.deleteUser(this.deleteTarget.id).subscribe({
      next: () => {
        if (this.usersPage) {
          this.usersPage = {
            ...this.usersPage,
            content: this.usersPage.content.filter(u => u.id !== this.deleteTarget!.id),
            totalElement: this.usersPage.totalElement - 1
          };
        }
        this.deleteConfirmOpen = false;
        this.deleteTarget = null;
        this.deleteInProgress = false;
      },
      error: () => { this.deleteInProgress = false; }
    });
  }

  cancelDelete(): void {
    this.deleteConfirmOpen = false;
    this.deleteTarget = null;
  }

  // ── Helpers ──────────────────────────────────────────────────────────────
  private replaceUser(updated: AdminUserResponse): void {
    if (this.usersPage) {
      this.usersPage = {
        ...this.usersPage,
        content: this.usersPage.content.map(u => u.id === updated.id ? updated : u)
      };
    }
    this.actionUserId = null;
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  getRoleBadgeClass(role: string): string {
    const map: Record<string, string> = { ADMIN: 'badge--admin', USER: 'badge--user' };
    return map[role] ?? 'badge--default';
  }
}

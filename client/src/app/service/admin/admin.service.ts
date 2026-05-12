import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminUserResponse, AdminUpdateUserRequest, AdminFeedbackResponse, AdminFeedbackRequest, AdminCommentRequest, AdminFeedbackStatus } from '../../interfaces/admin.interface';
import { PageResponse } from '../../interfaces/page.interface';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getUsers(page = 0, size = 20): Observable<PageResponse<AdminUserResponse>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdDate,desc');
    return this.http.get<PageResponse<AdminUserResponse>>(`${this.apiUrl}/users`, { params });
  }

  getUserById(id: number): Observable<AdminUserResponse> {
    return this.http.get<AdminUserResponse>(`${this.apiUrl}/users/${id}`);
  }

  updateUser(id: number, request: AdminUpdateUserRequest): Observable<AdminUserResponse> {
    return this.http.put<AdminUserResponse>(`${this.apiUrl}/users/${id}`, request);
  }

  activateUser(id: number): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.apiUrl}/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.apiUrl}/users/${id}/deactivate`, {});
  }

  lockUser(id: number): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.apiUrl}/users/${id}/lock`, {});
  }

  unlockUser(id: number): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.apiUrl}/users/${id}/unlock`, {});
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${id}`);
  }

  // ── Feedback ────────────────────────────────────────────────────────────────

  getFeedbacks(page = 0, size = 20): Observable<PageResponse<AdminFeedbackResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminFeedbackResponse>>(`${this.apiUrl}/feedbacks`, { params });
  }

  getFeedbacksByStatus(status: AdminFeedbackStatus, page = 0, size = 20): Observable<PageResponse<AdminFeedbackResponse>> {
    const params = new HttpParams().set('status', status).set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminFeedbackResponse>>(`${this.apiUrl}/feedbacks/by-status`, { params });
  }

  getFeedbackById(id: number): Observable<AdminFeedbackResponse> {
    return this.http.get<AdminFeedbackResponse>(`${this.apiUrl}/feedbacks/${id}`);
  }

  createFeedback(request: AdminFeedbackRequest): Observable<AdminFeedbackResponse> {
    return this.http.post<AdminFeedbackResponse>(`${this.apiUrl}/feedbacks`, request);
  }

  updateFeedback(id: number, request: AdminFeedbackRequest): Observable<AdminFeedbackResponse> {
    return this.http.put<AdminFeedbackResponse>(`${this.apiUrl}/feedbacks/${id}`, request);
  }

  deleteFeedback(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/feedbacks/${id}`);
  }

  addFeedbackComment(feedbackId: number, message: string): Observable<AdminFeedbackResponse> {
    const body: AdminCommentRequest = { message };
    return this.http.post<AdminFeedbackResponse>(`${this.apiUrl}/feedbacks/${feedbackId}/comments`, body);
  }

  deleteFeedbackComment(feedbackId: number, index: number): Observable<AdminFeedbackResponse> {
    return this.http.delete<AdminFeedbackResponse>(`${this.apiUrl}/feedbacks/${feedbackId}/comments/${index}`);
  }
}

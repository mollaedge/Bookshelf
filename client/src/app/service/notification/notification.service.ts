import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../interfaces/page.interface';
import { DtoNotificationResponse } from '../../interfaces/notification.interface';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private baseUrl = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getMyNotifications(page = 0, size = 20): Observable<PageResponse<DtoNotificationResponse>> {
    return this.http.get<PageResponse<DtoNotificationResponse>>(
      `${this.baseUrl}?page=${page}&size=${size}`
    );
  }

  getUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.baseUrl}/unread-count`);
  }

  markAsRead(notificationId: number): Observable<DtoNotificationResponse> {
    return this.http.patch<DtoNotificationResponse>(
      `${this.baseUrl}/${notificationId}/read`,
      {}
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/read-all`, {});
  }

  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${notificationId}`);
  }

  clearAll(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}`);
  }
}

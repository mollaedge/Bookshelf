import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AppFeedbackDto, AppFeedbackRequest } from '../../interfaces/feedback.interface';
import { PageResponse } from '../book/books.service';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private apiUrl = `${environment.apiUrl}/app-feedbacks`;

  constructor(private http: HttpClient) {}

  getAll(page: number = 0, size: number = 20): Observable<PageResponse<AppFeedbackDto>> {
    return this.http.get<PageResponse<AppFeedbackDto>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getMy(page: number = 0, size: number = 20): Observable<PageResponse<AppFeedbackDto>> {
    return this.http.get<PageResponse<AppFeedbackDto>>(`${this.apiUrl}/me?page=${page}&size=${size}`);
  }

  getById(id: number): Observable<AppFeedbackDto> {
    return this.http.get<AppFeedbackDto>(`${this.apiUrl}/${id}`);
  }

  submit(request: AppFeedbackRequest): Observable<AppFeedbackDto> {
    return this.http.post<AppFeedbackDto>(this.apiUrl, request);
  }

  edit(id: number, request: AppFeedbackRequest): Observable<AppFeedbackDto> {
    return this.http.put<AppFeedbackDto>(`${this.apiUrl}/${id}`, request);
  }

  toggleUpvote(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/upvote`, {});
  }

  addComment(id: number, message: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/comments`, { message });
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}

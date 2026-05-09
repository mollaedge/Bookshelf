import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DtoFriendPageResponse,
  DtoRelationResponse,
  DtoUserSearchResult,
  RelationPageResponse,
  UserSearchPageResponse
} from '../../interfaces/relation.interface';

@Injectable({
  providedIn: 'root'
})
export class RelationService {
  private baseUrl = `${environment.apiUrl}/relations`;

  constructor(private http: HttpClient) {}

  sendFriendRequest(targetUserId: number): Observable<DtoRelationResponse> {
    return this.http.post<DtoRelationResponse>(`${this.baseUrl}/friend-request/${targetUserId}`, {});
  }

  acceptFriendRequest(requestId: number): Observable<DtoRelationResponse> {
    return this.http.put<DtoRelationResponse>(`${this.baseUrl}/friend-request/${requestId}/accept`, {});
  }

  rejectFriendRequest(requestId: number): Observable<DtoRelationResponse> {
    return this.http.put<DtoRelationResponse>(`${this.baseUrl}/friend-request/${requestId}/reject`, {});
  }

  getIncomingFriendRequests(page: number = 0, size: number = 20): Observable<RelationPageResponse> {
    return this.http.get<RelationPageResponse>(`${this.baseUrl}/friend-requests/incoming?page=${page}&size=${size}`);
  }

  getOutgoingFriendRequests(page: number = 0, size: number = 20): Observable<RelationPageResponse> {
    return this.http.get<RelationPageResponse>(`${this.baseUrl}/friend-requests/outgoing?page=${page}&size=${size}`);
  }

  getFriends(page: number = 0, size: number = 20): Observable<RelationPageResponse> {
    return this.http.get<RelationPageResponse>(`${this.baseUrl}/friends?page=${page}&size=${size}`);
  }

  removeFriend(targetUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/friends/${targetUserId}`);
  }

  followUser(targetUserId: number): Observable<DtoRelationResponse> {
    return this.http.post<DtoRelationResponse>(`${this.baseUrl}/follow/${targetUserId}`, {});
  }

  unfollowUser(targetUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/follow/${targetUserId}`);
  }

  getFollowing(page: number = 0, size: number = 20): Observable<RelationPageResponse> {
    return this.http.get<RelationPageResponse>(`${this.baseUrl}/following?page=${page}&size=${size}`);
  }

  getFollowers(page: number = 0, size: number = 20): Observable<RelationPageResponse> {
    return this.http.get<RelationPageResponse>(`${this.baseUrl}/followers?page=${page}&size=${size}`);
  }

  searchUsers(query: string, page: number = 0, size: number = 20): Observable<UserSearchPageResponse> {
    return this.http.get<UserSearchPageResponse>(
      `${this.baseUrl}/users/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`
    );
  }

  viewUserPage(userId: number): Observable<DtoFriendPageResponse> {
    return this.http.get<DtoFriendPageResponse>(`${this.baseUrl}/users/${userId}`);
  }

  cancelOutgoingFriendRequest(targetUserId: number): Observable<void> {
    return this.removeFriend(targetUserId);
  }
}

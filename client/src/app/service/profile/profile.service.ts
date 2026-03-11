import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDashboardResponse, UpdateProfileRequest, UserProfileResponse } from '../../interfaces/user.interface';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = `${environment.apiUrl}/app-profile`;

  constructor(private http: HttpClient) { }

  getDashboard(): Observable<UserDashboardResponse> {
    return this.http.get<UserDashboardResponse>(`${this.apiUrl}/me`);
  }

  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.apiUrl}`+ `/profile`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}`+ `/profile`, request);
  }

  deleteProfile(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}`+ `/profile`);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDashboardResponse } from '../../interfaces/user.interface';

export interface UserProfileResponse {
  id: number;
  firstname: string;
  lastname: string;
  fullName: string;
  email: string;
  dateOfBirth: string;
  provider: string;
  accountLocked: boolean;
  enabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = `${environment.apiUrl}/app-profile`;

  constructor(private http: HttpClient) { }

  getDashboard(): Observable<UserDashboardResponse> {
    return this.http.get<UserDashboardResponse>(`${this.apiUrl}/me`);
  }

  updateProfile(profile: Partial<UserProfileResponse>): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}/profile`, profile);
  }
}

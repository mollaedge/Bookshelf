import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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
  private apiUrl = `${environment.apiUrl}/app-profile/profile`;

  // Note: Authorization headers are automatically added by AuthInterceptor
  constructor(private http: HttpClient) { }

  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(this.apiUrl);
  }

  updateProfile(profile: Partial<UserProfileResponse>): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(this.apiUrl, profile);
  }
}

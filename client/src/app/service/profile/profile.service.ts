import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDashboardResponse, UpdateProfileRequest, UserProfileResponse } from '../../interfaces/user.interface';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = `${environment.apiUrl}/app-profile`;
  
  private profilePictureUpdatedSource = new Subject<void>();
  profilePictureUpdated$ = this.profilePictureUpdatedSource.asObservable();

  constructor(private http: HttpClient) { }

  getDashboard(): Observable<UserDashboardResponse> {
    return this.http.get<UserDashboardResponse>(`${this.apiUrl}/me`);
  }

  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.apiUrl}/profile`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}/profile`, request);
  }

  deleteProfile(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/profile`);
  }

  uploadProfilePicture(file: File): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<void>(`${this.apiUrl}/profile/picture`, form).pipe(
      tap(() => this.profilePictureUpdatedSource.next())
    );
  }

  getProfilePicture(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/profile/picture`, { responseType: 'blob' });
  }

  uploadWallpaper(file: File): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<void>(`${this.apiUrl}/profile/wallpaper`, form);
  }

  getWallpaper(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/profile/wallpaper`, { responseType: 'blob' });
  }
}


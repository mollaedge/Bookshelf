import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthStateService } from './auth-state.service';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface AuthRequest {
  email: string;
  password: string;
}

interface AuthResponse {
  token: string;
  email?: string;
  userId?: string;
  user?: {
    email: string;
    givenName?: string;
    familyName?: string;
    [key: string]: any;  // For any other fields that might be in the user object
  };
  // Add other fields that your API returns
}

interface RegisterRequest {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private baseUrl = environment.apiUrl;
  private loginUrl = `${this.baseUrl}/auth/authenticate`;
  private registerUrl = `${this.baseUrl}/auth/register`;
  private googleUrl = `${this.baseUrl}/auth/google`;

  constructor(private http: HttpClient,
              private authState: AuthStateService,
              private router: Router
  ) {}

  authenticate(authRequest: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.loginUrl, authRequest).pipe(
      tap((response: AuthResponse) => {
        // Store user auth data
        this.authState.setUser({
          email: authRequest.email,
          token: response.token
          // Add any other user properties from the response
        });
      })
    );
  }

  register(registerRequest: RegisterRequest): Observable<any> {
    return this.http.post<any>(this.registerUrl, registerRequest).pipe(
      tap(response => {
        console.log('Registration successful, please login');
      })
    );
  }

  handleGoogleCredential(response: any) {
    if (!response || !response.credential) {
      console.error('Invalid Google Sign-In response');
      return;
    }
    
    const idToken = response.credential;
    
    this.http.post<AuthResponse>(this.googleUrl, { idToken })
      .subscribe({
        next: (res) => {
          if (!res || !res.token) {
            console.error('Invalid server response:', res);
            return;
          }
          
          // Store user info from the response
          this.authState.setUser({
            email: res.user?.email || '',
            token: res.token
            // Add any other user properties from the response
          });
          console.log('Google authentication successful');
          this.router.navigate(['/dashboard/dash']);
        },
        error: (err) => {
          console.error('Google login failed:', err);
        }
      });
  }
}

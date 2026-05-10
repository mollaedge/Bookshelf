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
  fullName?: string;
  firstname?: string;
  lastname?: string;
  userId?: string | number;
  user?: {
    id?: string | number;
    email: string;
    fullName?: string;
    name?: string;
    firstname?: string;
    lastname?: string;
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

  private toNumberId(value: unknown): number | undefined {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
  }

  private resolveFullName(response: AuthResponse): string | undefined {
    const candidates: Array<unknown> = [
      response.user?.fullName,
      response.user?.name,
      response.fullName
    ];

    for (const candidate of candidates) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate.trim();
      }
    }

    const firstName =
      response.user?.firstname ??
      response.user?.givenName ??
      response.firstname;
    const lastName =
      response.user?.lastname ??
      response.user?.familyName ??
      response.lastname;

    const composed = `${firstName ?? ''} ${lastName ?? ''}`.trim();
    return composed || undefined;
  }

  private resolveEmail(response: AuthResponse, fallbackEmail: string): string {
    return response.user?.email || response.email || fallbackEmail;
  }

  authenticate(authRequest: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.loginUrl, authRequest).pipe(
      tap((response: AuthResponse) => {
        const resolvedId = this.toNumberId(response.userId) ?? this.toNumberId(response.user?.id);
        const resolvedFullName = this.resolveFullName(response);
        const resolvedEmail = this.resolveEmail(response, authRequest.email);
        // Store user auth data
        this.authState.setUser({
          id: resolvedId,
          email: resolvedEmail,
          fullName: resolvedFullName,
          token: response.token
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

  activateAccount(token: string, email: string): Observable<void> {
    return this.http.get<void>(`${this.baseUrl}/auth/activate-account`, {
      params: { token, email }
    });
  }

  resendActivationToken(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/resend-activation`, null, {
      params: { email }
    });
  }

  handleGoogleCredential(response: any) {
    if (!response || !response.credential) {
      console.error('Invalid Google Sign-In response');
      return;
    }
    
    const idToken = response.credential;
    
    // Store Google token if needed for Google APIs
    localStorage.setItem('googleToken', idToken);
  

    this.http.post<AuthResponse>(this.googleUrl, { idToken })
      .subscribe({
        next: (res) => {
          if (!res || !res.token) {
            console.error('Invalid server response:', res);
            return;
          }

          const resolvedId = this.toNumberId(res.userId) ?? this.toNumberId(res.user?.id);
          const resolvedFullName = this.resolveFullName(res);
          this.authState.setUser({
            id: resolvedId,
            email: this.resolveEmail(res, ''),
            fullName: resolvedFullName,
            token: res.token
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

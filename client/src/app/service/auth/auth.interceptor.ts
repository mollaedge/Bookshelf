import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { isPlatformBrowser } from '@angular/common';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private authState: AuthStateService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let token = '';
    
    // Check if we're in browser context
    if (isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && window.localStorage) {
      // Try to get token from localStorage directly first (most reliable)
      token = localStorage.getItem('authToken') || '';
      
      // If not found, try from AuthStateService
      if (!token) {
        const user = this.authState.getUserFromStorage();
        if (user && user.token) {
          token = user.token;
        }
      }
      
      // Debug logging
      console.log('🔐 AuthInterceptor Debug:', {
        url: req.url,
        tokenExists: !!token,
        tokenLength: token ? token.length : 0,
        tokenPreview: token ? token.substring(0, 30) + '...' : 'NO TOKEN',
        hasAuthHeader: req.headers.has('Authorization')
      });
    }
    
    // Clone request and add authorization header if token exists
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('✅ Token added to request:', req.url);
    } else {
      console.warn('⚠️ No token available for request to:', req.url);
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        // Log error details
        if (isPlatformBrowser(this.platformId)) {
          console.error('HTTP Error:', {
            status: error.status,
            url: error.url,
            message: error.message,
            error: error.error
          });
        }
        
        // Check if it's a 401/403 error or JWT token expired message
        const isJwtExpired = error.error && 
                            typeof error.error === 'object' && 
                            error.error.error === 'JWT token is expired';
        
        if (error.status === 401 || error.status === 403 || isJwtExpired) {
          // Clear session cache
          this.authState.clearUser();
          
          // Clear all session storage and local storage
          if (typeof window !== 'undefined') {
            if (window.sessionStorage) {
              sessionStorage.clear();
            }
            if (window.localStorage) {
              // Clear all except the items already cleared by clearUser
              const keysToKeep: string[] = [];
              const allKeys: string[] = [];
              for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key) {
                  allKeys.push(key);
                }
              }
              allKeys.forEach(key => {
                if (!keysToKeep.includes(key)) {
                  localStorage.removeItem(key);
                }
              });
            }
          }
          
          // Redirect to login page
          this.router.navigate(['/auth']);
          
          const reason = isJwtExpired ? 'JWT token expired' : `${error.status} Unauthorized`;
          console.warn(`${reason} - Session cleared and redirected to login`);
          
          // Return empty observable to suppress error propagation
          return EMPTY;
        }
        
        return throwError(() => error);
      })
    );
  }
}

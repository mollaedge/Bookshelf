import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { catchError, throwError, EMPTY } from 'rxjs';
import { AuthStateService } from './auth-state.service';
import { environment } from '../../../environments/environment';

const PROTECTED_PATHS = ['/mybooks', '/profile', '/about', '/feedback'];

function clearAuthStorage(): void {
  if (typeof window === 'undefined') return;
  if (window.sessionStorage) sessionStorage.clear();
  if (window.localStorage) {
    ['authToken', 'authUser', 'googleToken'].forEach(key => localStorage.removeItem(key));
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Only intercept requests to our own API — pass external requests through untouched
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  const authState = inject(AuthStateService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  let token = '';

  // Check if we're in browser context
  if (isPlatformBrowser(platformId) && typeof window !== 'undefined' && window.localStorage) {
    // Try to get token from localStorage directly first (most reliable)
    token = localStorage.getItem('authToken') || '';

    // If not found, try from AuthStateService
    if (!token) {
      const user = authState.getUserFromStorage();
      if (user && user.token) {
        token = user.token;
      }
    }
  }

  // Clone request and add authorization header if token exists
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Network error or backend unreachable (status 0)
      if (error.status === 0) {
        const currentUrl = router.url;
        const isOnProtectedRoute = PROTECTED_PATHS.some(path => currentUrl.startsWith(path));

        if (isOnProtectedRoute) {
          authState.clearUser();
          clearAuthStorage();
          router.navigate(['/auth'], { queryParams: { reason: 'unavailable' } });
          return EMPTY;
        }

        return throwError(() => error);
      }

      // Check if it's a 401/403 error or JWT token expired message
      const isJwtExpired = error.error &&
                          typeof error.error === 'object' &&
                          error.error.error === 'JWT token is expired';

      if (error.status === 401 || error.status === 403 || isJwtExpired) {
        // Clear session cache
        authState.clearUser();
        clearAuthStorage();

        // Redirect to login page
        router.navigate(['/auth']);

        // Return empty observable to suppress error propagation
        return EMPTY;
      }

      return throwError(() => error);
    })
  );
};

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { catchError, throwError, EMPTY } from 'rxjs';
import { AuthStateService } from './auth-state.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
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
      // Check if it's a 401/403 error or JWT token expired message
      const isJwtExpired = error.error && 
                          typeof error.error === 'object' && 
                          error.error.error === 'JWT token is expired';
      
      if (error.status === 401 || error.status === 403 || isJwtExpired) {
        // Clear session cache
        authState.clearUser();
        
        // Clear all session storage and local storage
        if (typeof window !== 'undefined') {
          if (window.sessionStorage) {
            sessionStorage.clear();
          }
          if (window.localStorage) {
            const allKeys: string[] = [];
            for (let i = 0; i < localStorage.length; i++) {
              const key = localStorage.key(i);
              if (key) {
                allKeys.push(key);
              }
            }
            allKeys.forEach(key => {
              localStorage.removeItem(key);
            });
          }
        }
        
        // Redirect to login page
        router.navigate(['/auth']);
        
        // Return empty observable to suppress error propagation
        return EMPTY;
      }
      
      return throwError(() => error);
    })
  );
};

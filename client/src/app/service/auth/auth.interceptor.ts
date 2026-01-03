import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private authState: AuthStateService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Add token to request if available
    const user = this.authState.getUserFromStorage();
    if (user && user.token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${user.token}`
        }
      });
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
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

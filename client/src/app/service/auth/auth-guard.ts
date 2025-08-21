import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { Observable, map } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  constructor(
    private authState: AuthStateService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean> {
    return this.authState.user$.pipe(
      map(user => {
        if (user) {
          return true;
        } else {
          this.router.navigate(['/auth']);
          return false;
        }
      })
    );
  }
}
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AuthUser {
  email: string;
  token: string;
  // add other user fields as needed
}

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  private userSubject = new BehaviorSubject<AuthUser | null>(this.getUserFromStorage());

  user$: Observable<AuthUser | null> = this.userSubject.asObservable();

  setUser(user: AuthUser) {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('authToken', user.token);
      localStorage.setItem('authUser', JSON.stringify(user));
    }
    this.userSubject.next(user);
  }

  clearUser() {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
      localStorage.removeItem('googleToken');
    }
    this.userSubject.next(null);
  }

  getUserFromStorage(): AuthUser | null {
    if (typeof window !== 'undefined' && window.localStorage) {
      const user = localStorage.getItem('authUser');
      return user ? JSON.parse(user) : null;
    }
    return null;
  }
}

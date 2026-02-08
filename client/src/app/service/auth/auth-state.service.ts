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
  private userSubject!: BehaviorSubject<AuthUser | null>;
  user$!: Observable<AuthUser | null>;
  private cachedUser: AuthUser | null = null;

  constructor() {
    // Cache the user on initialization for fast access
    this.cachedUser = this.getUserFromStorage();
    this.userSubject = new BehaviorSubject<AuthUser | null>(this.cachedUser);
    this.user$ = this.userSubject.asObservable();
  }

  setUser(user: AuthUser) {
    this.cachedUser = user;
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('authToken', user.token);
      localStorage.setItem('authUser', JSON.stringify(user));
    }
    this.userSubject.next(user);
  }

  clearUser() {
    this.cachedUser = null;
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
      localStorage.removeItem('googleToken');
    }
    this.userSubject.next(null);
  }

  // Fast cached access - returns immediately without localStorage read
  getCurrentUser(): AuthUser | null {
    // If not cached yet and we're in browser, load it once
    if (!this.cachedUser && typeof window !== 'undefined' && window.localStorage) {
      const user = localStorage.getItem('authUser');
      this.cachedUser = user ? JSON.parse(user) : null;
    }
    return this.cachedUser;
  }

  // Returns immediately if cached, otherwise reads from storage
  getUserFromStorage(): AuthUser | null {
    if (this.cachedUser) {
      return this.cachedUser;
    }
    
    if (typeof window !== 'undefined' && window.localStorage) {
      const user = localStorage.getItem('authUser');
      this.cachedUser = user ? JSON.parse(user) : null;
      return this.cachedUser;
    }
    return null;
  }
}

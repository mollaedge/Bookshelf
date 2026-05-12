import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AuthUser {
  id?: number;
  email: string;
  token: string;
  fullName?: string;
  roles?: string[];
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
    this.hydrateUserIdFromToken();
    this.userSubject = new BehaviorSubject<AuthUser | null>(this.cachedUser);
    this.user$ = this.userSubject.asObservable();
  }

  private toNumberId(value: unknown): number | undefined {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
  }

  private hydrateUserIdFromToken(): void {
    if (!this.cachedUser) {
      return;
    }

    const token = this.cachedUser.token;
    if (!token || token.split('.').length < 2) {
      return;
    }

    try {
      const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = atob(payloadBase64);
      const payload = JSON.parse(json) as Record<string, unknown>;

      const idFromToken =
        this.toNumberId(payload['userId']) ??
        this.toNumberId(payload['id']) ??
        this.toNumberId(payload['sub']);

      const rawRoles = payload['roles'] ?? payload['authorities'] ?? payload['scope'];
      const roles: string[] = Array.isArray(rawRoles)
        ? rawRoles.map((r: unknown) => String(r).replace(/^ROLE_/, ''))
        : typeof rawRoles === 'string'
          ? rawRoles.split(/[\s,]+/).map(r => r.replace(/^ROLE_/, ''))
          : [];

      if (!idFromToken && roles.length === 0) {
        return;
      }

      this.cachedUser = {
        ...this.cachedUser,
        ...(idFromToken ? { id: idFromToken } : {}),
        ...(roles.length > 0 ? { roles } : {})
      };
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.setItem('authUser', JSON.stringify(this.cachedUser));
      }
    } catch {
      // Ignore malformed token payloads and continue without id.
    }
  }

  private extractRolesFromToken(token: string): string[] {
    if (!token || token.split('.').length < 2) return [];
    try {
      const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(payloadBase64)) as Record<string, unknown>;
      const raw = payload['roles'] ?? payload['authorities'] ?? payload['scope'];
      if (Array.isArray(raw)) return raw.map((r: unknown) => String(r).replace(/^ROLE_/, ''));
      if (typeof raw === 'string') return raw.split(/[\s,]+/).map(r => r.replace(/^ROLE_/, ''));
    } catch { /* ignore */ }
    return [];
  }

  setUser(user: AuthUser) {
    const roles = this.extractRolesFromToken(user.token);
    this.cachedUser = roles.length > 0 ? { ...user, roles } : user;
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('authToken', user.token);
      localStorage.setItem('authUser', JSON.stringify(this.cachedUser));
    }
    this.userSubject.next(this.cachedUser);
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
  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.roles?.includes('ADMIN') ?? false;
  }

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

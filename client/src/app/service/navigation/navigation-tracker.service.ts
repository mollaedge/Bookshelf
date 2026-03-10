import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NavigationTrackerService {
  private lastVisitedPage: string = '/dashboard';
  private readonly publicPaths = ['/dashboard', '/about', '/feedback', '/auth'];

  constructor(private router: Router) {
    // Track navigation changes
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.updateLastVisitedPage(event.url);
      });
  }

  private updateLastVisitedPage(url: string): void {
    // Clean the URL (remove query params and fragments)
    const cleanUrl = url.split('?')[0].split('#')[0];
    
    // Only update if it's a public page
    if (this.isPublicPage(cleanUrl)) {
      this.lastVisitedPage = cleanUrl;
    }
  }

  isPublicPage(url: string): boolean {
    return this.publicPaths.some(path => url.startsWith(path));
  }

  getPostLogoutRedirectUrl(): string {
    // Return the last visited page if it's public, otherwise return '/about'
    return this.isPublicPage(this.lastVisitedPage) ? this.lastVisitedPage : '/about';
  }

  setLastVisitedPage(url: string): void {
    if (this.isPublicPage(url)) {
      this.lastVisitedPage = url;
    }
  }
}
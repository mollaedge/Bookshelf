import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { NavigationTrackerService } from '../../service/navigation/navigation-tracker.service';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss'],
  standalone: false,
  animations: [
    trigger('slideAnimation', [
      transition(':enter', [
        style({ transform: 'translateY(-2%)', opacity: 0 }),
        animate('300ms ease-out', style({ transform: 'translateY(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ transform: 'translateY(-2%)', opacity: 0 }))
      ])
    ])
  ]
})
export class NavComponent {
  user$: Observable<any>;
  isNavCollapsed = false;
  showAuthModal = false;
  isRegisterView = false;
  showAddBookPopup = false;

  constructor(
    private authService: AuthStateService,
    private router: Router,
    private navigationTracker: NavigationTrackerService
  ) {
    this.user$ = this.authService.user$;
  }

  toggleNav(): void {
    this.isNavCollapsed = !this.isNavCollapsed;
  }

  openAuthModal(): void {
    this.showAuthModal = true;
    document.body.style.overflow = 'hidden';
  }

  closeAuthModal(): void {
    this.showAuthModal = false;
    document.body.style.overflow = 'auto';
  }

  setAuthView(isRegister: boolean): void {
    this.isRegisterView = isRegister;
  }

  logout(): void {
    // Clear the user session
    this.authService.clearUser();
    
    // Navigate to the appropriate page after logout
    const redirectUrl = this.navigationTracker.getPostLogoutRedirectUrl();
    this.router.navigate([redirectUrl]);
  }

  openAddBook(): void { this.showAddBookPopup = true; }
  closeAddBook(): void { this.showAddBookPopup = false; }
}

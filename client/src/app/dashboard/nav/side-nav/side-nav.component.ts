import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, Subscription } from 'rxjs';
import { AuthStateService, AuthUser } from '../../../service/auth/auth-state.service';

@Component({
  selector: 'app-side-nav',
  templateUrl: './side-nav.component.html',
  styleUrls: ['./side-nav.component.scss'],
  standalone: false
})
export class SideNavComponent implements OnInit, OnDestroy {
  user$: Observable<AuthUser | null>;
  isBrowser: boolean;
  collapsed = false;

  private userSub!: Subscription;

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  constructor(
    private authService: AuthStateService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.user$ = this.authService.user$;
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }
}

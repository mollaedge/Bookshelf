import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LoginService } from '../../../service/auth/login.service';

@Component({
  selector: 'app-activate-account',
  standalone: false,
  templateUrl: './activate-account.component.html',
  styleUrls: ['./activate-account.component.scss']
})
export class ActivateAccountComponent implements OnInit, OnDestroy {
  code = '';
  email = '';
  isLoading = false;
  isResending = false;
  errorMessage = '';
  successMessage = '';
  isActivated = false;

  countdown = 120;
  canResend = false;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private loginService: LoginService
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  private startTimer(): void {
    this.countdown = 120;
    this.canResend = false;
    this.clearTimer();
    this.timerInterval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        this.canResend = true;
        this.clearTimer();
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timerInterval !== null) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  get formattedCountdown(): string {
    const m = Math.floor(this.countdown / 60);
    const s = this.countdown % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  onActivate(): void {
    if (!this.code.trim()) return;
    this.isLoading = true;
    this.errorMessage = '';

    this.loginService.activateAccount(this.code.trim(), this.email).subscribe({
      next: () => {
        this.clearTimer();
        this.isActivated = true;
        setTimeout(() => this.router.navigate(['/auth/login']), 2500);
      },
      error: () => {
        this.errorMessage = 'Invalid or expired code. Please try again.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  onResend(): void {
    this.isResending = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.loginService.resendActivationToken(this.email).subscribe({
      next: () => {
        this.successMessage = 'A new code has been sent to your email.';
        this.startTimer();
      },
      error: () => {
        this.errorMessage = 'Failed to resend code. Please try again.';
      },
      complete: () => {
        this.isResending = false;
      }
    });
  }
}

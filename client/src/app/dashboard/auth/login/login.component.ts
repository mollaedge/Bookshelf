import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { LoginService } from '../../../service/auth/login.service';
import { environment } from '../../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

declare const google: any;
declare global {
  interface Window {
    handleGoogleSignIn?: (response: any) => void;
  }
}

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  hidePassword = true;
  isLoading = false;
  errorMessage = '';

  isBrowser: boolean;

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private loginService: LoginService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.initLoginForm();

    // Show message if redirected due to service unavailability
    this.route.queryParams.subscribe(params => {
      if (params['reason'] === 'unavailable') {
        this.errorMessage = 'The service is temporarily unavailable. Please try again later.';
      }
    });

    if (this.isBrowser) {
      setTimeout(() => {
        this.initGoogleSignIn();
      }, 1000);

      if (document.readyState === 'complete') {
        this.initGoogleSignIn();
      } else {
        window.addEventListener('load', () => {
          this.initGoogleSignIn();
        });
      }
    }
  }
  
  private initGoogleSignIn(): void {
    if (!this.isBrowser) {
      return;
    }

    window.handleGoogleSignIn = (response: any) => {
      this.loginService.handleGoogleCredential(response);
    };

    if (typeof google !== 'undefined' && google.accounts) {
      try {
        const currentOrigin = window.location.origin;
        console.log('Current origin:', currentOrigin);
        
        google.accounts.id.initialize({
          client_id: environment.googleClientId,
          callback: window.handleGoogleSignIn,
          context: 'signin',
          use_fedcm_for_prompt: true,
          error_callback: (error: any) => {
            console.error('Google Sign-In initialization error:', error);
          }
        });
        
        const buttonContainer = document.getElementById('google-button-container');
        
        if (buttonContainer) {
          google.accounts.id.renderButton(
            buttonContainer,
            { 
              theme: 'outline', 
              size: 'large',
              type: 'standard',
              text: 'signin_with',
              width: '100%'
            }
          );
          
          console.log('Google Sign-In initialized successfully');
        } else {
          console.warn('Google button container not found');
        }
      } catch (error) {
        console.error('Error during Google Sign-In initialization:', error);
      }
    } else {
      console.warn('Google Sign-In API not loaded yet. The button will handle initialization.');
    }
  }
  
  private initLoginForm(): void {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSignIn(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      const authRequest = {
        email: this.loginForm.value.email,
        password: this.loginForm.value.password
      };

      this.loginService.authenticate(authRequest).subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          if (error.status === 0) {
            this.errorMessage = 'Unable to reach the server. Please check your connection or try again later.';
          } else if (error.status === 401 || error.status === 403) {
            this.errorMessage = 'Invalid email or password.';
          } else {
            this.errorMessage = 'Sign in failed. Please try again.';
          }
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    }
  }

  signInWithGoogle(): void {
    if (!this.isBrowser) {
      console.warn('Google Sign-In is only available in browser environment');
      return;
    }
    
    try {
      this.isLoading = true;
      
      if (typeof google === 'undefined' || !google.accounts) {
        console.error('Google Sign-In API not loaded');
        this.isLoading = false;
        return;
      }
      
      google.accounts.id.prompt();
      
      setTimeout(() => {
        this.isLoading = false;
      }, 3000);
    } catch (error) {
      console.error('Error with Google Sign-In:', error);
      this.isLoading = false;
    }
  }
}

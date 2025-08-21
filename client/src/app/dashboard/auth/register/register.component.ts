import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { LoginService } from '../../../service/auth/login.service';
import { environment } from '../../../../environments/environment';

declare const google: any;
declare global {
  interface Window {
    handleGoogleSignIn?: (response: any) => void;
  }
}

@Component({
  selector: 'app-register',
  standalone: false,
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  isLoading = false;
  isBrowser: boolean;

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private loginService: LoginService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.initRegisterForm();
    
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

  private initRegisterForm(): void {
    this.registerForm = this.formBuilder.group({
      firstname: ['', [Validators.required]],
      lastname: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
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
          context: 'signup',
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
              text: 'signup_with',
              shape: 'rectangular',
              width: buttonContainer.offsetWidth, // This will make the button use the container's width
              logo_alignment: 'center'
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

  onRegister(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      
      const registerRequest = {
        firstname: this.registerForm.value.firstname,
        lastname: this.registerForm.value.lastname,
        email: this.registerForm.value.email,
        password: this.registerForm.value.password
      };
      
      this.loginService.register(registerRequest).subscribe({
        next: () => {
          console.log('Registration successful');
          // After successful registration, navigate to login page
          this.router.navigate(['/dashboard/auth/login']);
        },
        error: (error) => {
          console.error('Registration failed:', error);
          this.isLoading = false;
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    }
  }
}

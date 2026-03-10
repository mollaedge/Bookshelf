import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NavigationTrackerService } from './navigation-tracker.service';

describe('NavigationTrackerService', () => {
  let service: NavigationTrackerService;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const spy = jasmine.createSpyObj('Router', ['navigate'], { 
      events: { pipe: () => ({ subscribe: () => {} }) } 
    });

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: spy }
      ]
    });
    service = TestBed.inject(NavigationTrackerService);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should identify public pages correctly', () => {
    expect(service.isPublicPage('/dashboard')).toBeTruthy();
    expect(service.isPublicPage('/about')).toBeTruthy();
    expect(service.isPublicPage('/feedback')).toBeTruthy();
    expect(service.isPublicPage('/auth/login')).toBeTruthy();
    expect(service.isPublicPage('/mybooks')).toBeFalsy();
    expect(service.isPublicPage('/profile')).toBeFalsy();
  });

  it('should return /about for protected last visited page', () => {
    service.setLastVisitedPage('/mybooks');
    expect(service.getPostLogoutRedirectUrl()).toBe('/about');
  });

  it('should return last visited page if it is public', () => {
    service.setLastVisitedPage('/dashboard');
    expect(service.getPostLogoutRedirectUrl()).toBe('/dashboard');
    
    service.setLastVisitedPage('/feedback');
    expect(service.getPostLogoutRedirectUrl()).toBe('/feedback');
  });
});
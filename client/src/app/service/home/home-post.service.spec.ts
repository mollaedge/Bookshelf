import { TestBed } from '@angular/core/testing';
import { HomePostService } from './home-post.service';

describe('HomePostService', () => {
  let service: HomePostService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HomePostService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

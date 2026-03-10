import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { HomePostService } from '../../service/home/home-post.service';
import { of } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HomeComponent],
      imports: [HttpClientTestingModule],
      providers: [AuthStateService, HomePostService]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate reading progress correctly', () => {
    const reading = {
      id: 1,
      userId: 1,
      username: 'test',
      bookTitle: 'Test Book',
      bookCover: '',
      currentPage: 50,
      totalPages: 100,
      startedAt: new Date()
    };
    expect(component.getReadingProgress(reading)).toBe(50);
  });

  it('should format time ago correctly', () => {
    const now = new Date();
    const result = component.getTimeAgo(now.toISOString());
    expect(result).toBe('Just now');
  });
});

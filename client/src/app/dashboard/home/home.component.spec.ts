import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { DtoStreamInfo } from '../../service/stream/stream.service';
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
    const reading: DtoStreamInfo = {
      streamId: 1,
      hostId: 1,
      hostName: 'Test User',
      bookTitle: 'Test Book',
      bookCover: 'test.jpg',
      currentPage: 50,
      totalPages: 100,
      startedAt: new Date().toISOString(),
      watcherCount: 5,
      isActive: true
    };
    expect(component.getReadingProgress(reading)).toBe(50);
  });

  it('should format time ago correctly', () => {
    const now = new Date();
    const result = component.getTimeAgo(now.toISOString());
    expect(result).toBe('Just now');
  });
});

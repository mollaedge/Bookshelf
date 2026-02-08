import { Component, OnInit } from '@angular/core';
import { ProfileService, UserProfileResponse } from '../../service/profile/profile.service';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { Router } from '@angular/router';

interface UserProfile {
  name: string;
  email: string;
  joinDate: Date;
  avatar: string;
  bio: string;
  location: string;
  booksRead: number;
  currentlyReading: number;
  toRead: number;
  favoriteGenres: string[];
}

interface ReadingActivity {
  month: string;
  booksRead: number;
}

interface GenreDistribution {
  genre: string;
  count: number;
  percentage: number;
  color: string;
}

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  user: UserProfile = {
    name: '',
    email: '',
    joinDate: new Date(),
    avatar: '',
    bio: 'Passionate reader and book enthusiast. Love exploring different genres and sharing my reading experiences.',
    location: '',
    booksRead: 127,
    currentlyReading: 3,
    toRead: 45,
    favoriteGenres: ['Fiction', 'Science Fiction', 'Mystery', 'Biography']
  };

  isLoading = true;
  backendProfile: UserProfileResponse | null = null;

  stats = [
    { label: 'Books Read', value: 127, icon: '📚', color: '#4CAF50' },
    { label: 'Currently Reading', value: 3, icon: '📖', color: '#2196F3' },
    { label: 'Want to Read', value: 45, icon: '⭐', color: '#FF9800' },
    { label: 'Reading Streak', value: 15, icon: '🔥', color: '#F44336', suffix: ' days' }
  ];

  readingActivity: ReadingActivity[] = [
    { month: 'Jan', booksRead: 8 },
    { month: 'Feb', booksRead: 12 },
    { month: 'Mar', booksRead: 15 },
    { month: 'Apr', booksRead: 10 },
    { month: 'May', booksRead: 14 },
    { month: 'Jun', booksRead: 11 },
    { month: 'Jul', booksRead: 13 },
    { month: 'Aug', booksRead: 16 },
    { month: 'Sep', booksRead: 9 },
    { month: 'Oct', booksRead: 12 },
    { month: 'Nov', booksRead: 7 },
    { month: 'Dec', booksRead: 0 }
  ];

  genreDistribution: GenreDistribution[] = [
    { genre: 'Fiction', count: 45, percentage: 35, color: '#1976D2' },
    { genre: 'Science Fiction', count: 30, percentage: 24, color: '#64B5F6' },
    { genre: 'Mystery', count: 25, percentage: 20, color: '#0D47A1' },
    { genre: 'Biography', count: 15, percentage: 12, color: '#90CAF9' },
    { genre: 'Other', count: 12, percentage: 9, color: '#BBDEFB' }
  ];

  recentAchievements = [
    { title: 'Bookworm', description: 'Read 100 books', icon: '🏆', date: new Date('2024-11-01') },
    { title: 'Speed Reader', description: 'Finished 5 books in a week', icon: '⚡', date: new Date('2024-10-15') },
    { title: 'Genre Explorer', description: 'Read books from 10 different genres', icon: '🌟', date: new Date('2024-09-20') },
    { title: 'Consistent Reader', description: '30-day reading streak', icon: '📅', date: new Date('2024-08-10') }
  ];

  readingGoals = [
    { title: 'Annual Reading Goal', current: 127, target: 150, unit: 'books' },
    { title: 'Monthly Goal', current: 7, target: 12, unit: 'books' },
    { title: 'Reading Time', current: 85, target: 100, unit: 'hours' }
  ];

  isEditMode = false;
  editedUser: UserProfile = { ...this.user };

  constructor(
    private profileService: ProfileService,
    private authState: AuthStateService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Try to load profile immediately - will use cached token or fetch from backend
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.backendProfile = profile;
        this.user = {
          name: profile.fullName || `${profile.firstname} ${profile.lastname}`,
          email: profile.email,
          joinDate: profile.dateOfBirth ? new Date(profile.dateOfBirth) : new Date(),
          avatar: '',
          bio: this.user.bio,
          location: this.user.location,
          booksRead: this.user.booksRead,
          currentlyReading: this.user.currentlyReading,
          toRead: this.user.toRead,
          favoriteGenres: this.user.favoriteGenres
        };
        this.editedUser = { ...this.user };
        this.isLoading = false;
      },
      error: (error) => {
        // If profile fetch fails (no auth), redirect to login
        this.isLoading = false;
        this.router.navigate(['/auth']);
      }
    });
  }

  getMaxReading(): number {
    return Math.max(...this.readingActivity.map(a => a.booksRead));
  }

  getBarHeight(count: number): number {
    const max = this.getMaxReading();
    return (count / max) * 100;
  }

  getProgressPercentage(current: number, target: number): number {
    return Math.min((current / target) * 100, 100);
  }

  toggleEditMode(): void {
    this.isEditMode = !this.isEditMode;
    if (this.isEditMode) {
      this.editedUser = { ...this.user };
    }
  }

  saveProfile(): void {
    this.user = { ...this.editedUser };
    this.isEditMode = false;
    
    // Update backend if needed
    if (this.backendProfile) {
      const updatedProfile: Partial<UserProfileResponse> = {
        firstname: this.user.name.split(' ')[0],
        lastname: this.user.name.split(' ').slice(1).join(' '),
        email: this.user.email
      };
      
      this.profileService.updateProfile(updatedProfile).subscribe({
        next: (profile) => {
          console.log('Profile updated successfully:', profile);
          this.backendProfile = profile;
        },
        error: (error) => {
          console.error('Error updating profile:', error);
        }
      });
    }
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editedUser = { ...this.user };
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  getMemberDuration(): string {
    const now = new Date();
    const joined = new Date(this.user.joinDate);
    const diffTime = Math.abs(now.getTime() - joined.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays < 30) return `${diffDays} days`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} months`;
    return `${Math.floor(diffDays / 365)} years`;
  }

  getStrokeDashOffset(index: number): number {
    let offset = 0;
    for (let i = 0; i < index; i++) {
      offset += this.genreDistribution[i].percentage * 2.51;
    }
    return 251 - offset;
  }
}

import { Component, OnInit } from '@angular/core';
import { ProfileService, UserProfileResponse } from '../../service/profile/profile.service';
import { UserDashboardResponse } from '../../interfaces/user.interface';

interface UserProfile {
  name: string;
  email: string;
  joinDate: Date;
  avatar: string;
  bio: string;
  location: string;
  favoriteGenres: string[];
}

interface ProfileReadingActivity {
  month: string;
  booksRead: number;
}

interface ProfileGenreDistribution {
  genre: string;
  count: number;
  percentage: number;
  color: string;
}

const GENRE_COLORS = ['#1976D2', '#64B5F6', '#0D47A1', '#90CAF9', '#BBDEFB', '#42A5F5', '#1565C0'];

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
    bio: '',
    location: '',
    favoriteGenres: []
  };

  isLoading = true;
  profileError: string = '';
  saveError: string = '';
  dashboard: UserDashboardResponse | null = null;

  stats: { label: string; value: number | string; icon: string; color: string; suffix?: string }[] = [];

  readingActivity: ProfileReadingActivity[] = [];

  genreDistribution: ProfileGenreDistribution[] = [];

  recentAchievements = [
    { title: 'Bookworm', description: 'Read 100 books', icon: '📚', date: new Date('2024-11-01') },
    { title: 'Speed Reader', description: 'Finished 5 books in a week', icon: '⚡', date: new Date('2024-10-15') },
    { title: 'Genre Explorer', description: 'Read books from 10 different genres', icon: '🌟', date: new Date('2024-09-20') },
    { title: 'Consistent Reader', description: '30-day reading streak', icon: '📅', date: new Date('2024-08-10') }
  ];

  readingGoals = [
    { title: 'Annual Reading Goal', current: 0, target: 150, unit: 'books' },
    { title: 'Monthly Goal', current: 0, target: 12, unit: 'books' }
  ];

  isEditMode = false;
  editedUser: UserProfile = { ...this.user };

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    this.profileError = '';

    this.profileService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.mapDashboardToView(data);
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.profileError = 'Failed to load profile. Please try again.';
      }
    });
  }

  private mapDashboardToView(data: UserDashboardResponse): void {
    this.user = {
      name: data.fullName || `${data.firstname} ${data.lastname}`,
      email: data.email,
      joinDate: data.joinDate ? new Date(data.joinDate) : new Date(),
      avatar: '',
      bio: data.bio ?? '',
      location: data.location ?? '',
      favoriteGenres: data.genreDistribution.slice(0, 4).map(g => g.genre)
    };
    this.editedUser = { ...this.user };

    this.stats = [
      { label: 'Books Owned', value: data.stats.booksOwned, icon: '📚', color: '#4CAF50' },
      { label: 'Currently Borrowed', value: data.stats.currentlyBorrowed, icon: '📖', color: '#2196F3' },
      { label: 'Books Read', value: data.stats.booksRead, icon: '⭐', color: '#FF9800' },
      { label: 'Reading Streak', value: data.readingStreak, icon: '🔥', color: '#F44336', suffix: ' days' }
    ];

    this.readingActivity = data.readingActivity.map(a => ({
      month: this.formatMonth(a.month),
      booksRead: a.booksRead
    }));

    const totalRead = data.genreDistribution.reduce((sum, g) => sum + g.count, 0);
    this.genreDistribution = data.genreDistribution.map((g, i) => ({
      genre: g.genre,
      count: g.count,
      percentage: totalRead > 0 ? Math.round((g.count / totalRead) * 100) : g.percentage,
      color: GENRE_COLORS[i % GENRE_COLORS.length]
    }));

    this.readingGoals = [
      { title: 'Annual Reading Goal', current: data.stats.booksRead, target: 150, unit: 'books' },
      { title: 'Monthly Goal', current: data.stats.booksRead % 12, target: 12, unit: 'books' }
    ];
  }

  /** Converts "YYYY-MM" to abbreviated month name e.g. "Jan" */
  private formatMonth(yearMonth: string): string {
    const [year, month] = yearMonth.split('-');
    const date = new Date(+year, +month - 1);
    return date.toLocaleString('default', { month: 'short' });
  }

  get booksRead(): number {
    return this.dashboard?.stats?.booksRead ?? 0;
  }

  getMaxReading(): number {
    const max = Math.max(...this.readingActivity.map(a => a.booksRead));
    return max === 0 ? 1 : max;
  }

  getBarHeight(count: number): number {
    return (count / this.getMaxReading()) * 100;
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

    if (this.dashboard) {
      const updatedProfile: Partial<UserProfileResponse> = {
        firstname: this.user.name.split(' ')[0],
        lastname: this.user.name.split(' ').slice(1).join(' '),
        email: this.user.email
      };

      this.profileService.updateProfile(updatedProfile).subscribe({
        next: () => {
          this.saveError = '';
        },
        error: () => {
          this.saveError = 'Failed to save profile. Please try again.';
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

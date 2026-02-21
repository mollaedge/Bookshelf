export interface BookStats {
  booksOwned: number;
  booksRead: number;
  currentlyBorrowed: number;
  returnedBooks: number;
  pendingRequests: number;
}

export interface ReadingActivity {
  month: string; // "YYYY-MM" from BE
  booksRead: number;
}

export interface GenreDistribution {
  genre: string;
  count: number;
  percentage: number;
}

export interface UserDashboardResponse {
  id: number;
  firstname: string;
  lastname: string;
  fullName: string;
  email: string;
  joinDate: string;
  bio?: string;
  location?: string;
  provider: string;
  stats: BookStats;
  readingActivity: ReadingActivity[];
  genreDistribution: GenreDistribution[];
  readingStreak: number;
}

import { Component, OnInit } from '@angular/core';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { HomePostService, HomePost } from '../../service/home/home-post.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

interface LiveReading {
  id: number;
  userId: number;
  username: string;
  bookTitle: string;
  bookCover: string;
  currentPage: number;
  totalPages: number;
  startedAt: Date;
}

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  user$: Observable<any>;
  userPosts: HomePost[] = [];
  liveReadings: LiveReading[] = [];
  loading: boolean = false;
  postsLoading: boolean = false;
  error: string = '';
  currentPage: number = 0;
  pageSize: number = 15;
  totalPages: number = 0;
  isLastPage: boolean = false;
  
  // Create post modal
  showCreatePostModal: boolean = false;
  editingPost: HomePost | null = null;

  // Image lightbox/preview
  showLightbox: boolean = false;
  lightboxImageUrl: string = '';
  lightboxImageName: string = '';

  constructor(
    private authState: AuthStateService,
    private homePostService: HomePostService
  ) {
    this.user$ = this.authState.user$;
  }

  ngOnInit(): void {
    this.loadUserPosts();
    this.loadLiveReadings();
  }

  loadUserPosts(page: number = 0): void {
    this.postsLoading = true;
    this.error = '';
    
    this.homePostService.getAllPosts(page, this.pageSize).subscribe({
      next: (response) => {
        this.userPosts = response.content;
        this.currentPage = response.number;
        this.totalPages = response.totalPages;
        this.isLastPage = response.last;
        this.postsLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading posts:', error);
        this.error = 'Failed to load posts. Please try again later.';
        this.postsLoading = false;
        // Fallback to empty array
        this.userPosts = [];
      }
    });
  }

  loadLiveReadings(): void {
    // Mock data for now - this would be a different API endpoint for live reading sessions
    this.liveReadings = [
      {
        id: 1,
        userId: 4,
        username: 'AvidReader',
        bookTitle: 'The Hobbit',
        bookCover: 'https://via.placeholder.com/150x200',
        currentPage: 156,
        totalPages: 310,
        startedAt: new Date('2026-03-10T11:20:00')
      },
      {
        id: 2,
        userId: 5,
        username: 'BookWorm42',
        bookTitle: 'Pride and Prejudice',
        bookCover: 'https://via.placeholder.com/150x200',
        currentPage: 89,
        totalPages: 432,
        startedAt: new Date('2026-03-10T10:45:00')
      },
      {
        id: 3,
        userId: 6,
        username: 'PageTurner',
        bookTitle: 'Dune',
        bookCover: 'https://via.placeholder.com/150x200',
        currentPage: 234,
        totalPages: 688,
        startedAt: new Date('2026-03-10T09:30:00')
      }
    ];
  }

  getReadingProgress(reading: LiveReading): number {
    return Math.round((reading.currentPage / reading.totalPages) * 100);
  }

  getTimeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  }

  // Pagination
  loadNextPage(): void {
    if (!this.isLastPage) {
      this.loadUserPosts(this.currentPage + 1);
    }
  }

  loadPreviousPage(): void {
    if (this.currentPage > 0) {
      this.loadUserPosts(this.currentPage - 1);
    }
  }

  // Create Post Modal
  openCreatePostModal(): void {
    this.editingPost = null;
    this.showCreatePostModal = true;
  }

  openEditPostModal(post: HomePost): void {
    this.editingPost = post;
    this.showCreatePostModal = true;
  }

  closeCreatePostModal(): void {
    this.showCreatePostModal = false;
    this.editingPost = null;
  }

  onPostCreated(): void {
    this.loadUserPosts(0); // Reload posts from first page
  }

  // Delete Post
  deletePost(post: HomePost): void {
    if (confirm('Are you sure you want to delete this post?')) {
      this.homePostService.deletePost(post.id).subscribe({
        next: () => {
          console.log('Post deleted successfully');
          this.loadUserPosts(this.currentPage);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error deleting post:', error);
          this.error = 'Failed to delete post. You may not have permission.';
        }
      });
    }
  }

  // Check if current user is post owner
  isPostOwner(post: HomePost): boolean {
    const currentUser = this.authState.getCurrentUser();
    
    if (!currentUser || !currentUser.email) {
      return false;
    }
    
    if (!post.authorEmail) {
      return false;
    }
    
    // Compare emails (case-insensitive)
    return currentUser.email.toLowerCase() === post.authorEmail.toLowerCase();
  }

  // Image Lightbox/Preview
  openLightbox(imageUrl: string, imageName: string): void {
    this.lightboxImageUrl = imageUrl;
    this.lightboxImageName = imageName;
    this.showLightbox = true;
    // Prevent body scroll when lightbox is open
    document.body.style.overflow = 'hidden';
  }

  closeLightbox(): void {
    this.showLightbox = false;
    this.lightboxImageUrl = '';
    this.lightboxImageName = '';
    // Restore body scroll
    document.body.style.overflow = 'auto';
  }

  downloadAttachment(dataUri: string, fileName: string): void {
    const link = document.createElement('a');
    link.href = dataUri;
    link.download = fileName;
    link.click();
  }
}

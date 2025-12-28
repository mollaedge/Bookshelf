import { Component, OnInit } from '@angular/core';
import { FeedbackCard, Comment } from '../../interfaces/feedback.interface';

@Component({
  selector: 'app-feedback',
  standalone: false,
  templateUrl: './feedback.component.html',
  styleUrl: './feedback.component.scss'
})
export class FeedbackComponent implements OnInit {
  feedbackCards: FeedbackCard[] = [];
  filteredCards: FeedbackCard[] = [];
  selectedFilter: string = 'all';
  sortBy: string = 'newest';
  showAddModal: boolean = false;
  showCommentModal: boolean = false;
  selectedCard: FeedbackCard | null = null;
  
  // New feedback form
  newFeedback = {
    title: '',
    description: '',
    category: 'feature' as 'feature' | 'bug' | 'improvement' | 'other'
  };
  
  // New comment
  newComment: string = '';
  
  // Mock current user
  currentUser: string = 'User123'; // In real app, get from auth service
  
  ngOnInit(): void {
    // Load mock data - In a real app, this would come from a service/API
    this.loadMockData();
    this.applyFilters();
  }
  
  loadMockData(): void {
    this.feedbackCards = [
      {
        id: '1',
        title: 'Dark Mode Support',
        description: 'It would be great to have a dark mode option for better reading at night.',
        category: 'feature',
        upvotes: 15,
        upvotedBy: ['User1', 'User2', 'User3'],
        comments: [
          {
            id: '1',
            text: 'This would be amazing! My eyes would thank you.',
            author: 'User1',
            createdAt: new Date('2025-12-20')
          }
        ],
        author: 'User1',
        createdAt: new Date('2025-12-15'),
        status: 'under-review'
      },
      {
        id: '2',
        title: 'Book Search Not Working',
        description: 'The search feature returns no results when I search for books by author.',
        category: 'bug',
        upvotes: 8,
        upvotedBy: ['User4', 'User5'],
        comments: [],
        author: 'User4',
        createdAt: new Date('2025-12-18'),
        status: 'open'
      },
      {
        id: '3',
        title: 'Improve Loading Speed',
        description: 'The app takes too long to load the book list. Can we optimize this?',
        category: 'improvement',
        upvotes: 12,
        upvotedBy: ['User6', 'User7', 'User8'],
        comments: [
          {
            id: '2',
            text: 'Yes! Sometimes it takes 5+ seconds.',
            author: 'User6',
            createdAt: new Date('2025-12-19')
          },
          {
            id: '3',
            text: 'Pagination might help.',
            author: 'User7',
            createdAt: new Date('2025-12-20')
          }
        ],
        author: 'User6',
        createdAt: new Date('2025-12-16'),
        status: 'planned'
      }
    ];
  }
  
  applyFilters(): void {
    let filtered = [...this.feedbackCards];
    
    // Apply category filter
    if (this.selectedFilter !== 'all') {
      filtered = filtered.filter(card => card.category === this.selectedFilter);
    }
    
    // Apply sorting
    switch (this.sortBy) {
      case 'newest':
        filtered.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
        break;
      case 'oldest':
        filtered.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());
        break;
      case 'most-upvoted':
        filtered.sort((a, b) => b.upvotes - a.upvotes);
        break;
      case 'most-comments':
        filtered.sort((a, b) => b.comments.length - a.comments.length);
        break;
    }
    
    this.filteredCards = filtered;
  }
  
  onFilterChange(filter: string): void {
    this.selectedFilter = filter;
    this.applyFilters();
  }
  
  onSortChange(sort: string): void {
    this.sortBy = sort;
    this.applyFilters();
  }
  
  openAddModal(): void {
    this.showAddModal = true;
    this.newFeedback = {
      title: '',
      description: '',
      category: 'feature'
    };
  }
  
  closeAddModal(): void {
    this.showAddModal = false;
  }
  
  submitFeedback(): void {
    if (!this.newFeedback.title.trim() || !this.newFeedback.description.trim()) {
      alert('Please fill in all fields');
      return;
    }
    
    const newCard: FeedbackCard = {
      id: Date.now().toString(),
      title: this.newFeedback.title,
      description: this.newFeedback.description,
      category: this.newFeedback.category,
      upvotes: 0,
      upvotedBy: [],
      comments: [],
      author: this.currentUser,
      createdAt: new Date(),
      status: 'open'
    };
    
    this.feedbackCards.unshift(newCard);
    this.applyFilters();
    this.closeAddModal();
  }
  
  toggleUpvote(card: FeedbackCard): void {
    const index = card.upvotedBy.indexOf(this.currentUser);
    
    if (index > -1) {
      // User already upvoted, remove upvote
      card.upvotedBy.splice(index, 1);
      card.upvotes--;
    } else {
      // Add upvote
      card.upvotedBy.push(this.currentUser);
      card.upvotes++;
    }
    
    this.applyFilters();
  }
  
  hasUpvoted(card: FeedbackCard): boolean {
    return card.upvotedBy.includes(this.currentUser);
  }
  
  openComments(card: FeedbackCard): void {
    this.selectedCard = card;
    this.showCommentModal = true;
    this.newComment = '';
  }
  
  closeCommentModal(): void {
    this.showCommentModal = false;
    this.selectedCard = null;
    this.newComment = '';
  }
  
  submitComment(): void {
    if (!this.newComment.trim() || !this.selectedCard) {
      return;
    }
    
    const comment: Comment = {
      id: Date.now().toString(),
      text: this.newComment,
      author: this.currentUser,
      createdAt: new Date()
    };
    
    this.selectedCard.comments.push(comment);
    this.newComment = '';
    this.applyFilters();
  }
  
  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'feature': '#4CAF50',
      'bug': '#f44336',
      'improvement': '#FF9800',
      'other': '#9E9E9E'
    };
    return colors[category] || '#9E9E9E';
  }
  
  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'open': '#2196F3',
      'under-review': '#FF9800',
      'planned': '#9C27B0',
      'completed': '#4CAF50'
    };
    return colors[status] || '#2196F3';
  }
  
  formatDate(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;
    if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
    return date.toLocaleDateString();
  }
}

export interface FeedbackCard {
  id: string;
  title: string;
  description: string;
  category: 'feature' | 'bug' | 'improvement' | 'other';
  upvotes: number;
  upvotedBy: string[];
  comments: Comment[];
  author: string;
  createdAt: Date;
  status: 'open' | 'under-review' | 'planned' | 'completed';
}

export interface Comment {
  id: string;
  text: string;
  author: string;
  createdAt: Date;
}

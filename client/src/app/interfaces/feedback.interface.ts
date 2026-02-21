export type FeedbackStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface CommentDto {
  authorId: number;
  fullName: string;
  message: string;
  createdAt: string;
}

export interface AppFeedbackDto {
  id: number;
  title: string;
  description: string;
  status: FeedbackStatus;
  upvoteCount: number;
  upvotedByCurrentUser: boolean;
  ownFeedback: boolean;
  age: string;
  author?: string;
  comments: CommentDto[];
}

export interface AppFeedbackRequest {
  title: string;
  description: string;
}

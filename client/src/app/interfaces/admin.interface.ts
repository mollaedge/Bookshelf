export interface AdminUserResponse {
  id: number;
  firstname: string;
  lastname: string;
  fullName: string;
  email: string;
  bio?: string;
  location?: string;
  provider: string;
  dateOfBirth?: string;
  accountLocked: boolean;
  enabled: boolean;
  roles: string[];
  createdDate: string;
  lastModifiedDate: string;
}

export interface AdminUpdateUserRequest {
  firstname?: string;
  lastname?: string;
  email?: string;
  bio?: string;
  location?: string;
  dateOfBirth?: string;
  accountLocked?: boolean;
  enabled?: boolean;
  roles?: string[];
}

// ── Feedback ──────────────────────────────────────────────────────────────────

export type AdminFeedbackStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface AdminDtoComment {
  authorId?: number;
  fullName?: string;
  authorName?: string;
  message: string;
  createdAt: string;
}

export interface AdminFeedbackResponse {
  id: number;
  title: string;
  description: string;
  status: AdminFeedbackStatus;
  upvoteCount: number;
  upvotedByCurrentUser: boolean;
  ownFeedback: boolean;
  age: string;
  createdDate: string;
  createdBy?: number;
  authorName?: string;
  comments: AdminDtoComment[];
}

export interface AdminFeedbackRequest {
  title: string;
  description: string;
  status?: AdminFeedbackStatus;
}

export interface AdminCommentRequest {
  message: string;
}


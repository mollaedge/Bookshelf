export interface PostAttachment {
  id: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  uploadedAt: string;
  /** Ready-to-use Base64 data URI — use as src/href directly */
  dataUri: string;
}

export interface HomePost {
  id: number;
  title: string;
  content: string;
  createdDate: string;
  lastModifiedDate?: string;
  authorName: string;
  ownerId: number;
  authorEmail: string;
  attachments: PostAttachment[];
  likeCount?: number;
  commentCount?: number;
  shareCount?: number;
  likedByCurrentUser?: boolean;

  // Backward-compatible aliases for older API payloads
  likesCount?: number;
  commentsCount?: number;
}

export interface DtoPostCommentRequest {
  content: string;
}

export interface DtoPostCommentResponse {
  id: number;
  content: string;
  authorId: number;
  authorName: string;
  authorEmail: string;
  createdDate: string;
  lastModifiedDate: string;
}

export interface DtoPostLikeResponse {
  likeCount: number;
  likedByCurrentUser: boolean;
}

export interface CreatePostRequest {
  title: string;
  content: string;
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
}
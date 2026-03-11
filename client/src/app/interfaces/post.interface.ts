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
  likesCount?: number;
  commentsCount?: number;
}

export interface CreatePostRequest {
  title: string;
  content: string;
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
}
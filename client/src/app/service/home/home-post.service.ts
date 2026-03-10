import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../book/books.service';

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

@Injectable({
  providedIn: 'root'
})
export class HomePostService {
  private apiUrl = environment.apiUrl;
  private baseUrl = `${this.apiUrl}/home/posts`;

  constructor(private http: HttpClient) { }

  // =========================================================================
  // CREATE
  // =========================================================================

  /**
   * Create a new post with optional file attachments
   */
  createPost(request: CreatePostRequest, files?: File[]): Observable<number> {
    const formData = new FormData();
    
    // Add post data as JSON blob
    const postBlob = new Blob([JSON.stringify(request)], { type: 'application/json' });
    formData.append('post', postBlob);
    
    // Add files if present
    if (files && files.length > 0) {
      files.forEach(file => {
        formData.append('files', file);
      });
    }
    
    return this.http.post<number>(this.baseUrl, formData);
  }

  // =========================================================================
  // READ
  // =========================================================================

  /**
   * Get a single post by its ID
   */
  getPostById(postId: number): Observable<HomePost> {
    return this.http.get<HomePost>(`${this.baseUrl}/${postId}`);
  }

  /**
   * Get all posts, ordered by date descending (newest first), paged
   */
  getAllPosts(page: number = 0, size: number = 15): Observable<PageResponse<HomePost>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<HomePost>>(this.baseUrl, { params });
  }

  /**
   * Get posts belonging to the authenticated user, newest first
   */
  getMyPosts(page: number = 0, size: number = 15): Observable<PageResponse<HomePost>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<HomePost>>(`${this.baseUrl}/my`, { params });
  }

  /**
   * Get the URL for a post attachment
   */
  getAttachmentUrl(postId: number, attachmentId: number): string {
    return `${this.baseUrl}/${postId}/attachments/${attachmentId}`;
  }

  /**
   * Download a post attachment
   */
  getAttachment(postId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${postId}/attachments/${attachmentId}`, {
      responseType: 'blob'
    });
  }

  // =========================================================================
  // UPDATE
  // =========================================================================

  /**
   * Update a post's title and/or content. Only the post author can do this.
   */
  updatePost(postId: number, request: UpdatePostRequest): Observable<HomePost> {
    return this.http.put<HomePost>(`${this.baseUrl}/${postId}`, request);
  }

  /**
   * Add more attachments to an existing post. Only the post author can do this.
   */
  addAttachments(postId: number, files: File[]): Observable<HomePost> {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    
    return this.http.post<HomePost>(`${this.baseUrl}/${postId}/attachments`, formData);
  }

  // =========================================================================
  // DELETE
  // =========================================================================

  /**
   * Delete an entire post (including all its attachments). Only the author can do this.
   */
  deletePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${postId}`);
  }

  /**
   * Remove a single attachment from a post. Only the author can do this.
   */
  deleteAttachment(postId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${postId}/attachments/${attachmentId}`);
  }
}

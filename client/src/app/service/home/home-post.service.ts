import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../interfaces/page.interface';
import {
  HomePost,
  CreatePostRequest,
  UpdatePostRequest,
  DtoPostCommentRequest,
  DtoPostCommentResponse,
  DtoPostLikeResponse
} from '../../interfaces/post.interface';

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

  // =========================================================================
  // LIKES & COMMENTS
  // =========================================================================

  /**
   * Toggle like on a post (like/unlike).
   */
  toggleLike(postId: number): Observable<DtoPostLikeResponse> {
    return this.http.post<DtoPostLikeResponse>(`${this.baseUrl}/${postId}/likes`, {});
  }

  /**
   * Get current like status for a post.
   */
  getLikeStatus(postId: number): Observable<DtoPostLikeResponse> {
    return this.http.get<DtoPostLikeResponse>(`${this.baseUrl}/${postId}/likes`);
  }

  /**
   * Add a comment to a post.
   */
  addComment(postId: number, request: DtoPostCommentRequest): Observable<DtoPostCommentResponse> {
    return this.http.post<DtoPostCommentResponse>(`${this.baseUrl}/${postId}/comments`, request);
  }

  /**
   * Get paged comments for a post.
   */
  getComments(postId: number, page: number = 0, size: number = 20): Observable<PageResponse<DtoPostCommentResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<DtoPostCommentResponse>>(`${this.baseUrl}/${postId}/comments`, { params });
  }
}

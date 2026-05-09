import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { EventSourcePolyfill } from 'event-source-polyfill';
import {
  DtoMessageResponse,
  DtoConversationResponse,
  DtoMessageRequest,
} from '../../interfaces/message.interface';
import { PageResponse } from '../../interfaces/page.interface';
import { Subject, Observable } from 'rxjs';
import { AuthStateService } from '../auth/auth-state.service';

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  private apiUrl = `${environment.apiUrl}/messages`;
  private sseEmitter: EventSource | EventSourcePolyfill | null = null;
  private messageSubject = new Subject<DtoMessageResponse>();
  private messageReadSubject = new Subject<DtoMessageResponse>();

  public message$ = this.messageSubject.asObservable();
  public messageRead$ = this.messageReadSubject.asObservable();

  constructor(
    private http: HttpClient,
    private authState: AuthStateService
  ) {}

  /**
   * Establish SSE connection for real-time messaging.
   * Call this once on login.
   */
  connectSSE(): void {
    if (this.sseEmitter) {
      return; // Already connected
    }

    const token = this.authState.getCurrentUser()?.token;

    // Use header-auth SSE for JWT-based auth setups. Fallback to native EventSource if token is unavailable.
    if (token) {
      this.sseEmitter = new EventSourcePolyfill(`${this.apiUrl}/connect`, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
        withCredentials: true,
        heartbeatTimeout: 120000,
      });
    } else {
      this.sseEmitter = new EventSource(`${this.apiUrl}/connect`, {
        withCredentials: true,
      });
    }

    // Listen for incoming messages
    this.sseEmitter.addEventListener('NEW_MESSAGE', (event: MessageEvent<string>) => {
      try {
        const message = JSON.parse(event.data) as DtoMessageResponse;
        this.messageSubject.next(message);
      } catch (error) {
        console.error('Failed to parse NEW_MESSAGE event:', error);
      }
    });

    // Listen for message read notifications
    this.sseEmitter.addEventListener('MESSAGE_READ', (event: MessageEvent<string>) => {
      try {
        const message = JSON.parse(event.data) as DtoMessageResponse;
        this.messageReadSubject.next(message);
      } catch (error) {
        console.error('Failed to parse MESSAGE_READ event:', error);
      }
    });

    // Ignore heartbeat
    this.sseEmitter.addEventListener('heartbeat', () => {
      // Keep-alive – ignore
    });

    // Handle connection errors
    this.sseEmitter.onerror = (error: Event) => {
      const readyState = this.sseEmitter?.readyState;
      console.error('SSE connection error:', { error, readyState, url: `${this.apiUrl}/connect` });

      // CLOSED (2) means terminal failure; clean up so next action can reconnect.
      if (readyState === 2) {
        this.disconnectSSE();
      }
    };
  }

  /**
   * Close SSE connection (call on logout).
   */
  disconnectSSE(): void {
    if (this.sseEmitter) {
      this.sseEmitter.close();
      this.sseEmitter = null;
    }
  }

  /**
   * Send a message to a friend.
   */
  sendMessage(
    friendId: number,
    request: DtoMessageRequest
  ): Observable<DtoMessageResponse> {
    return this.http.post<DtoMessageResponse>(
      `${this.apiUrl}/${friendId}`,
      request
    );
  }

  /**
   * Get all conversations (inbox) for the current user.
   */
  getConversations(
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<DtoConversationResponse>> {
    return this.http.get<PageResponse<DtoConversationResponse>>(
      `${this.apiUrl}/conversations`,
      {
        params: { page: page.toString(), size: size.toString() },
      }
    );
  }

  /**
   * Get message history with a specific friend.
   * Automatically marks unread messages from the friend as read.
   */
  getMessages(
    friendId: number,
    page: number = 0,
    size: number = 50
  ): Observable<PageResponse<DtoMessageResponse>> {
    return this.http.get<PageResponse<DtoMessageResponse>>(
      `${this.apiUrl}/conversations/${friendId}`,
      {
        params: { page: page.toString(), size: size.toString() },
      }
    );
  }

  /**
   * Mark a message as read.
   */
  markAsRead(messageId: number): Observable<DtoMessageResponse> {
    return this.http.patch<DtoMessageResponse>(
      `${this.apiUrl}/${messageId}/read`,
      {}
    );
  }

  /**
   * Delete a message (sender only).
   */
  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${messageId}`);
  }
}

import { Injectable, NgZone } from '@angular/core';
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

  /** Emits every incoming message received via SSE. */
  public message$ = this.messageSubject.asObservable();
  /** Emits whenever a message is marked read via SSE. */
  public messageRead$ = this.messageReadSubject.asObservable();

  private refreshUnreadCountSubject = new Subject<void>();
  /** Emits when components should refresh their unread conversation counts. */
  public refreshUnreadCount$ = this.refreshUnreadCountSubject.asObservable();

  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectDelay = 2000;   // start at 2 s
  private readonly maxDelay = 60000; // cap at 60 s
  private intentionalDisconnect = false;

  constructor(
    private http: HttpClient,
    private authState: AuthStateService,
    private ngZone: NgZone
  ) {}

  /**
   * Establish SSE connection for real-time messaging.
   * Automatically reconnects with exponential back-off on failure.
   * Call this once on login; call disconnectSSE() on logout.
   */
  connectSSE(): void {
    if (this.sseEmitter) {
      return; // Already connected
    }

    this.intentionalDisconnect = false;
    this._openSSE();
  }

  private _openSSE(): void {
    const token = this.authState.getCurrentUser()?.token;
    if (!token) return; // Not logged in — do not attempt

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

    // Reset back-off on a successful open
    this.sseEmitter.addEventListener('open', () => {
      this.reconnectDelay = 2000;
    });

    // Listen for incoming messages
    this.sseEmitter.addEventListener('NEW_MESSAGE', (event: MessageEvent<string>) => {
      try {
        const message = JSON.parse(event.data) as DtoMessageResponse;
        this.ngZone.run(() => this.messageSubject.next(message));
      } catch (error) {
        console.error('Failed to parse NEW_MESSAGE event:', error);
      }
    });

    // Listen for message read notifications
    this.sseEmitter.addEventListener('MESSAGE_READ', (event: MessageEvent<string>) => {
      try {
        const message = JSON.parse(event.data) as DtoMessageResponse;
        this.ngZone.run(() => this.messageReadSubject.next(message));
      } catch (error) {
        console.error('Failed to parse MESSAGE_READ event:', error);
      }
    });

    // Ignore heartbeat
    this.sseEmitter.addEventListener('heartbeat', () => { /* keep-alive */ });

    // Handle connection errors — reconnect unless intentionally disconnected
    this.sseEmitter.onerror = () => {
      this.sseEmitter?.close();
      this.sseEmitter = null;

      if (!this.intentionalDisconnect) {
        this._scheduleReconnect();
      }
    };
  }

  private _scheduleReconnect(): void {
    if (this.reconnectTimer) return;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (!this.intentionalDisconnect) {
        this._openSSE();
        // Double the delay for next failure, capped at maxDelay
        this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxDelay);
      }
    }, this.reconnectDelay);
  }

  /**
   * Close SSE connection permanently (call on logout).
   */
  disconnectSSE(): void {
    this.intentionalDisconnect = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.sseEmitter) {
      this.sseEmitter.close();
      this.sseEmitter = null;
    }
    this.reconnectDelay = 2000;
  }

  /**
   * Send a message to a friend.
   */
  sendMessage(
    friendId: number,
    request: DtoMessageRequest & { media?: File | null }
  ): Observable<DtoMessageResponse> {
    const formData = new FormData();
    formData.append('content', request.content);
    if (request.replyToId) formData.append('replyToId', request.replyToId.toString());
    if (request.media) formData.append('media', request.media);
    return this.http.post<DtoMessageResponse>(
      `${this.apiUrl}/${friendId}`,
      formData
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
   * Returns the number of conversations with unread messages.
   */
  getUnreadConversationCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/conversations/unread-count`);
  }

  /**
   * Notifies all subscribers that the unread count has changed.
   */
  notifyUnreadCountChanged(): void {
    this.refreshUnreadCountSubject.next();
  }

  /**
   * Delete a message (sender only).
   */
  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${messageId}`);
  }
}

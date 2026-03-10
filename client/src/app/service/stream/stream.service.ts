import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DtoStreamInfo {
  streamId: number;
  hostId: number;
  hostName: string;
  bookTitle?: string;
  bookCover?: string;
  currentPage?: number;
  totalPages?: number;
  startedAt: string;
  watcherCount: number;
  isActive: boolean;
}

export interface DtoStreamStartRequest {
  title: string;
}

export interface DtoSignalRequest {
  targetUserId?: number;
  signalType: 'offer' | 'answer' | 'ice-candidate';
  payload: string;
}

export interface StreamEvent {
  type: 'STREAM_STARTED' | 'WATCHER_JOINED' | 'WATCHER_LEFT' | 'STREAM_STOPPED' | 'SIGNAL';
  streamId: number;
  actorId: number;
  actorName: string;
  payload?: string;
  watcherCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class StreamService {
  private baseUrl = `${environment.apiUrl}/streams`;
  private abortControllers: Map<string, AbortController> = new Map();

  constructor(
    private http: HttpClient,
    private ngZone: NgZone
  ) {}

  // =========================================================================
  // HOST — start / stop
  // =========================================================================

  /**
   * Start a new live stream. Returns an Observable of SSE events.
   */
  startStream(request: DtoStreamStartRequest): Observable<StreamEvent> {
    const subject = new Subject<StreamEvent>();
    const url = `${this.baseUrl}/start?title=${encodeURIComponent(request.title)}`;
    
    // Use fetch-based SSE with Authorization header
    this.connectSSE(url, 'host', subject);
    
    return subject.asObservable();
  }

  /**
   * Stop the authenticated user's stream.
   */
  stopStream(): Observable<void> {
    // Close SSE connection if exists
    const controller = this.abortControllers.get('host');
    if (controller) {
      controller.abort();
      this.abortControllers.delete('host');
    }
    
    return this.http.delete<void>(`${this.baseUrl}/stop`);
  }

  // =========================================================================
  // WATCHER — join / leave
  // =========================================================================

  /**
   * Join an active stream as a watcher. Returns an Observable of SSE events.
   */
  joinStream(hostId: number): Observable<StreamEvent> {
    const subject = new Subject<StreamEvent>();
    const url = `${this.baseUrl}/${hostId}/join`;
    
    // Use fetch-based SSE with Authorization header
    this.connectSSE(url, `watcher-${hostId}`, subject);
    
    return subject.asObservable();
  }

  /**
   * Leave a stream (watcher only).
   */
  leaveStream(hostId: number): Observable<void> {
    // Close SSE connection if exists
    const controller = this.abortControllers.get(`watcher-${hostId}`);
    if (controller) {
      controller.abort();
      this.abortControllers.delete(`watcher-${hostId}`);
    }
    
    return this.http.delete<void>(`${this.baseUrl}/${hostId}/leave`);
  }

  // =========================================================================
  // SIGNALLING (WebRTC SDP / ICE)
  // =========================================================================

  /**
   * Send a WebRTC signalling message.
   */
  signal(hostId: number, request: DtoSignalRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${hostId}/signal`, request);
  }

  // =========================================================================
  // DISCOVERY
  // =========================================================================

  /**
   * List all currently active streams.
   */
  listStreams(): Observable<DtoStreamInfo[]> {
    return this.http.get<DtoStreamInfo[]>(this.baseUrl);
  }

  /**
   * Get info/metadata for a specific active stream.
   */
  getStreamInfo(hostId: number): Observable<DtoStreamInfo> {
    return this.http.get<DtoStreamInfo>(`${this.baseUrl}/${hostId}`);
  }

  // =========================================================================
  // CLEANUP
  // =========================================================================

  /**
   * Close all SSE connections.
   */
  closeAllConnections(): void {
    this.abortControllers.forEach((controller) => {
      controller.abort();
    });
    this.abortControllers.clear();
  }

  // =========================================================================
  // PRIVATE HELPERS
  // =========================================================================

  /**
   * Connect to an SSE endpoint using fetch API with Authorization header.
   */
  private async connectSSE(url: string, connectionKey: string, subject: Subject<StreamEvent>): Promise<void> {
    const token = localStorage.getItem('authToken');
    const abortController = new AbortController();
    this.abortControllers.set(connectionKey, abortController);

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'text/event-stream',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        signal: abortController.signal
      });

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status} ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Failed to get response reader');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      // Read the stream
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          this.abortControllers.delete(connectionKey);
          subject.complete();
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // Keep incomplete line in buffer

        let eventType = '';
        let eventData = '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim();
          } else if (line.startsWith('data:')) {
            eventData = line.substring(5).trim();
          } else if (line === '' && eventType && eventData) {
            // Complete event received
            try {
              const parsedData = JSON.parse(eventData);
              this.ngZone.run(() => {
                subject.next(parsedData);
              });
            } catch (e) {
              console.error('Failed to parse SSE data:', e);
            }
            eventType = '';
            eventData = '';
          }
        }
      }
    } catch (error: any) {
      if (error.name === 'AbortError') {
        console.log('SSE connection aborted');
        subject.complete();
      } else {
        this.ngZone.run(() => {
          console.error('SSE Error:', error);
          subject.error(error);
        });
      }
      this.abortControllers.delete(connectionKey);
    }
  }
}

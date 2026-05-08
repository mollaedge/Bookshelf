import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DtoIceServer, DtoSignalRequest } from '../../interfaces/stream.interface';
import { StreamService } from '../stream/stream.service';

@Injectable({
  providedIn: 'root'
})
export class WebRTCService {
  private peerConnections: Map<number, RTCPeerConnection> = new Map();
  private pendingCandidates: Map<number, RTCIceCandidateInit[]> = new Map();
  private iceServersCache: DtoIceServer[] | null = null;

  // Fallback used only if the backend is unreachable
  private readonly fallbackIceServers: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ];

  constructor(private streamService: StreamService) {}

  /**
   * Fetch ICE server config from the backend (cached after first call).
   */
  private async getIceConfig(): Promise<RTCConfiguration> {
    if (this.iceServersCache) {
      return { iceServers: this.iceServersCache as RTCIceServer[] };
    }
    try {
      const servers = await firstValueFrom(this.streamService.getIceServers());
      this.iceServersCache = servers;
      return { iceServers: servers as RTCIceServer[] };
    } catch (err) {
      console.warn('Could not fetch ICE servers from backend, using fallback:', err);
      return { iceServers: this.fallbackIceServers };
    }
  }

  /**
   * Create a peer connection for a specific user
   */
  async createPeerConnection(
    userId: number,
    onIceCandidate: (candidate: RTCIceCandidate) => void,
    onTrack?: (stream: MediaStream) => void
  ): Promise<RTCPeerConnection> {
    const config = await this.getIceConfig();
    const peerConnection = new RTCPeerConnection(config);

    // Handle ICE candidates
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('ICE candidate generated for user:', userId);
        onIceCandidate(event.candidate);
      }
    };

    // Handle incoming tracks (for watchers)
    // Use a single MediaStream to accumulate all tracks (audio + video)
    if (onTrack) {
      const remoteStream = new MediaStream();
      peerConnection.ontrack = (event) => {
        console.log('Received remote track from user:', userId, event.track.kind);
        // Prefer event.streams[0] if available, otherwise add track directly
        if (event.streams && event.streams[0]) {
          event.streams[0].getTracks().forEach(t => {
            if (!remoteStream.getTracks().find(e => e.id === t.id)) {
              remoteStream.addTrack(t);
            }
          });
        } else {
          remoteStream.addTrack(event.track);
        }
        onTrack(remoteStream);
      };
    }

    // Handle connection state changes
    peerConnection.onconnectionstatechange = () => {
      console.log(`Connection state with user ${userId}:`, peerConnection.connectionState);
    };

    // Handle ICE connection state changes
    peerConnection.oniceconnectionstatechange = () => {
      console.log(`ICE connection state with user ${userId}:`, peerConnection.iceConnectionState);
      
      if (peerConnection.iceConnectionState === 'failed' || 
          peerConnection.iceConnectionState === 'disconnected' ||
          peerConnection.iceConnectionState === 'closed') {
        console.warn(`Connection issues with user ${userId}`);
      }
    };

    this.peerConnections.set(userId, peerConnection);
    return peerConnection;
  }

  /**
   * Add local media stream to peer connection (host side)
   */
  addLocalStream(userId: number, stream: MediaStream): void {
    const peerConnection = this.peerConnections.get(userId);
    if (!peerConnection) {
      console.error('Peer connection not found for user:', userId);
      return;
    }

    stream.getTracks().forEach(track => {
      peerConnection.addTrack(track, stream);
      console.log('Added track to peer connection for user:', userId, track.kind);
    });
  }

  /**
   * Create and send an offer (host side)
   */
  async createOffer(userId: number, hostId: number): Promise<void> {
    const peerConnection = this.peerConnections.get(userId);
    if (!peerConnection) {
      console.error('Peer connection not found for user:', userId);
      return;
    }

    console.log('Creating offer for user:', userId, 'using hostId:', hostId);

    try {
      const offer = await peerConnection.createOffer({
        offerToReceiveAudio: false,
        offerToReceiveVideo: false
      });
      await peerConnection.setLocalDescription(offer);

      console.log('Offer created and set as local description:', offer);

      const signalRequest: DtoSignalRequest = {
        targetUserId: userId,
        signalType: 'offer',
        payload: JSON.stringify(offer)
      };

      console.log('Sending signal request to /streams/' + hostId + '/signal:', signalRequest);

      this.streamService.signal(hostId, signalRequest).subscribe({
        next: () => console.log('✓ Offer sent successfully to user:', userId),
        error: (error) => console.error('✗ Error sending offer:', error)
      });
    } catch (error) {
      console.error('Error creating offer:', error);
    }
  }

  /**
   * Handle received offer and create answer (watcher side)
   */
  async handleOffer(
    hostId: number,
    offer: RTCSessionDescriptionInit
  ): Promise<void> {
    const peerConnection = this.peerConnections.get(hostId);
    if (!peerConnection) {
      console.error('Peer connection not found for host:', hostId);
      return;
    }

    console.log('Handling offer from host:', hostId, offer);

    try {
      await peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
      console.log('Remote description set successfully');

      // Flush any ICE candidates that arrived before the offer
      await this.flushPendingCandidates(hostId);

      const answer = await peerConnection.createAnswer();
      await peerConnection.setLocalDescription(answer);

      console.log('Answer created and set as local description:', answer);

      const signalRequest: DtoSignalRequest = {
        targetUserId: hostId,
        signalType: 'answer',
        payload: JSON.stringify(answer)
      };

      console.log('Sending answer to host via /streams/' + hostId + '/signal:', signalRequest);

      this.streamService.signal(hostId, signalRequest).subscribe({
        next: () => console.log('✓ Answer sent successfully to host:', hostId),
        error: (error) => console.error('✗ Error sending answer:', error)
      });
    } catch (error) {
      console.error('Error handling offer:', error);
    }
  }

  /**
   * Handle received answer (host side)
   */
  async handleAnswer(
    userId: number,
    answer: RTCSessionDescriptionInit
  ): Promise<void> {
    const peerConnection = this.peerConnections.get(userId);
    if (!peerConnection) {
      console.error('Peer connection not found for user:', userId);
      return;
    }

    try {
      await peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
      console.log('Answer from user', userId, 'processed successfully');
      // Flush any ICE candidates queued before the answer arrived
      await this.flushPendingCandidates(userId);
    } catch (error) {
      console.error('Error handling answer:', error);
    }
  }

  /**
   * Handle received ICE candidate — queues it if remote description is not yet set
   */
  async handleIceCandidate(
    userId: number,
    candidate: RTCIceCandidateInit
  ): Promise<void> {
    const peerConnection = this.peerConnections.get(userId);
    if (!peerConnection) {
      console.error('Peer connection not found for user:', userId);
      return;
    }

    if (!peerConnection.remoteDescription) {
      // Remote description not set yet — buffer this candidate
      const queue = this.pendingCandidates.get(userId) ?? [];
      queue.push(candidate);
      this.pendingCandidates.set(userId, queue);
      console.log('ICE candidate queued (no remote description yet) for user:', userId);
      return;
    }

    try {
      await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      console.log('ICE candidate added for user:', userId);
    } catch (error) {
      console.error('Error adding ICE candidate:', error);
    }
  }

  /**
   * Flush queued ICE candidates after remote description is set
   */
  private async flushPendingCandidates(userId: number): Promise<void> {
    const queue = this.pendingCandidates.get(userId);
    if (!queue || queue.length === 0) return;
    this.pendingCandidates.delete(userId);

    const peerConnection = this.peerConnections.get(userId);
    if (!peerConnection) return;

    console.log(`Flushing ${queue.length} queued ICE candidates for user:`, userId);
    for (const candidate of queue) {
      try {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      } catch (error) {
        console.error('Error adding queued ICE candidate:', error);
      }
    }
  }

  /**
   * Send ICE candidate to remote peer
   */
  sendIceCandidate(
    hostId: number,
    targetUserId: number,
    candidate: RTCIceCandidate
  ): void {
    const signalRequest: DtoSignalRequest = {
      targetUserId: targetUserId,
      signalType: 'ice-candidate',
      payload: JSON.stringify(candidate.toJSON())
    };

    console.log('Sending ICE candidate to user', targetUserId, 'via /streams/' + hostId + '/signal');

    this.streamService.signal(hostId, signalRequest).subscribe({
      next: () => console.log('✓ ICE candidate sent to user:', targetUserId),
      error: (error) => console.error('✗ Error sending ICE candidate:', error)
    });
  }

  /**
   * Close peer connection with specific user
   */
  closePeerConnection(userId: number): void {
    const peerConnection = this.peerConnections.get(userId);
    if (peerConnection) {
      peerConnection.close();
      this.peerConnections.delete(userId);
      console.log('Closed peer connection with user:', userId);
    }
    this.pendingCandidates.delete(userId);
  }

  /**
   * Close all peer connections
   */
  closeAllPeerConnections(): void {
    this.peerConnections.forEach((connection, userId) => {
      connection.close();
      console.log('Closed peer connection with user:', userId);
    });
    this.peerConnections.clear();
    this.pendingCandidates.clear();
  }

  /**
   * Get peer connection for a user
   */
  getPeerConnection(userId: number): RTCPeerConnection | undefined {
    return this.peerConnections.get(userId);
  }

  /**
   * Check if peer connection exists for a user
   */
  hasPeerConnection(userId: number): boolean {
    return this.peerConnections.has(userId);
  }
}

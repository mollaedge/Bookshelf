import { Injectable } from '@angular/core';
import { StreamService, DtoSignalRequest } from '../stream/stream.service';

interface PeerConnection {
  connection: RTCPeerConnection;
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class WebRTCService {
  private peerConnections: Map<number, RTCPeerConnection> = new Map();
  private configuration: RTCConfiguration = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' }
    ]
  };

  constructor(private streamService: StreamService) {}

  /**
   * Create a peer connection for a specific user
   */
  createPeerConnection(
    userId: number,
    onIceCandidate: (candidate: RTCIceCandidate) => void,
    onTrack?: (stream: MediaStream) => void
  ): RTCPeerConnection {
    const peerConnection = new RTCPeerConnection(this.configuration);

    // Handle ICE candidates
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('ICE candidate generated for user:', userId);
        onIceCandidate(event.candidate);
      }
    };

    // Handle incoming tracks (for watchers)
    if (onTrack) {
      peerConnection.ontrack = (event) => {
        console.log('Received remote track from user:', userId);
        if (event.streams && event.streams[0]) {
          onTrack(event.streams[0]);
        }
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
    } catch (error) {
      console.error('Error handling answer:', error);
    }
  }

  /**
   * Handle received ICE candidate
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

    try {
      await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      console.log('ICE candidate added for user:', userId);
    } catch (error) {
      console.error('Error adding ICE candidate:', error);
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

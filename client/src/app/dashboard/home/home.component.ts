import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthStateService } from '../../service/auth/auth-state.service';
import { HomePostService, HomePost } from '../../service/home/home-post.service';
import { StreamService, DtoStreamInfo, DtoStreamStartRequest } from '../../service/stream/stream.service';
import { WebRTCService } from '../../service/webrtc/webrtc.service';
import { Observable, Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

type LiveReading = DtoStreamInfo;

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit, OnDestroy {
  user$: Observable<any>;
  userPosts: HomePost[] = [];
  liveReadings: LiveReading[] = [];
  loading: boolean = false;
  postsLoading: boolean = false;
  liveReadingsLoading: boolean = false;
  error: string = '';
  currentPage: number = 0;
  pageSize: number = 15;
  totalPages: number = 0;
  isLastPage: boolean = false;
  
  // Create post modal
  showCreatePostModal: boolean = false;
  editingPost: HomePost | null = null;

  // Image lightbox/preview
  showLightbox: boolean = false;
  lightboxImageUrl: string = '';
  lightboxImageName: string = '';

  // Streaming (as host)
  isStreaming: boolean = false;
  currentStreamId: number | null = null;
  currentHostId: number | null = null; // Host's user ID
  streamWatcherCount: number = 0;
  showStartStreamModal: boolean = false;
  localStream: MediaStream | null = null;
  videoElement: HTMLVideoElement | null = null;

  // Watching (as viewer)
  isWatching: boolean = false;
  watchedStreamHostId: number | null = null;
  watchedStreamHostName: string = '';
  remoteVideoElement: HTMLVideoElement | null = null;

  // Subscriptions
  private subscriptions: Subscription = new Subscription();

  constructor(
    private authState: AuthStateService,
    private homePostService: HomePostService,
    private streamService: StreamService,
    private webrtcService: WebRTCService
  ) {
    this.user$ = this.authState.user$;
  }

  ngOnInit(): void {
    this.loadUserPosts();
    this.loadLiveStreams();
  }

  ngOnDestroy(): void {
    if (this.isStreaming) {
      this.stopStream();
    }
    if (this.isWatching) {
      this.leaveStream();
    }
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
    }
    this.subscriptions.unsubscribe();
    this.streamService.closeAllConnections();
    this.webrtcService.closeAllPeerConnections();
  }

  loadUserPosts(page: number = 0): void {
    this.postsLoading = true;
    this.error = '';
    
    this.homePostService.getAllPosts(page, this.pageSize).subscribe({
      next: (response) => {
        this.userPosts = response.content;
        this.currentPage = response.number;
        this.totalPages = response.totalPages;
        this.isLastPage = response.last;
        this.postsLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading posts:', error);
        this.error = 'Failed to load posts. Please try again later.';
        this.postsLoading = false;
        // Fallback to empty array
        this.userPosts = [];
      }
    });
  }

  loadLiveStreams(): void {
    this.liveReadingsLoading = true;
    
    const sub = this.streamService.listStreams().subscribe({
      next: (streams) => {
        this.liveReadings = streams;
        this.liveReadingsLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading streams:', error);
        this.liveReadingsLoading = false;
        this.liveReadings = [];
      }
    });
    
    this.subscriptions.add(sub);
  }

  // Streaming methods
  openStartStreamModal(): void {
    this.showStartStreamModal = true;
  }

  closeStartStreamModal(): void {
    this.showStartStreamModal = false;
  }

  async onStreamStart(title: string): Promise<void> {

    try {
      // Request camera and microphone access
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 }
        },
        audio: true
      });

      const request: DtoStreamStartRequest = {
        title: title
      };

      const sub = this.streamService.startStream(request).subscribe({
        next: (event) => {
          console.log('Stream event:', event);
          
          if (event.type === 'STREAM_STARTED') {
            this.isStreaming = true;
            this.currentStreamId = event.streamId;
            this.currentHostId = event.actorId; // Store host user ID
            this.streamWatcherCount = event.watcherCount || 0;
            this.closeStartStreamModal();
            
            console.log('Stream started - streamId:', this.currentStreamId, 'hostId:', this.currentHostId);
            
            // Set video element source after a short delay to ensure DOM is ready
            setTimeout(() => {
              this.initVideoElement();
            }, 100);
          } else if (event.type === 'WATCHER_JOINED') {
            this.streamWatcherCount = event.watcherCount || 0;
            console.log('WATCHER_JOINED event received:', event);
            // Create peer connection and send offer to new watcher
            this.handleWatcherJoined(event.actorId);
          } else if (event.type === 'WATCHER_LEFT') {
            this.streamWatcherCount = event.watcherCount || 0;
            // Clean up peer connection for watcher who left
            this.webrtcService.closePeerConnection(event.actorId);
          } else if (event.type === 'SIGNAL') {
            // Handle signaling from watchers (answers, ICE candidates)
            this.handleSignalAsHost(event);
          }
        },
        error: (error) => {
          console.error('Streaming error:', error);
          this.error = 'Failed to start stream. Please try again.';
          this.isStreaming = false;
          if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
            this.localStream = null;
          }
        }
      });

      this.subscriptions.add(sub);
    } catch (error) {
      console.error('Error accessing camera/microphone:', error);
      this.error = 'Failed to access camera/microphone. Please grant permissions.';
    }
  }

  private initVideoElement(): void {
    this.videoElement = document.getElementById('localVideo') as HTMLVideoElement;
    if (this.videoElement && this.localStream) {
      this.videoElement.srcObject = this.localStream;
      this.videoElement.play();
    }
  }

  stopStream(): void {
    if (!this.isStreaming) return;

    // Stop local media tracks
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }

    if (this.videoElement) {
      this.videoElement.srcObject = null;
      this.videoElement = null;
    }

    this.streamService.stopStream().subscribe({
      next: () => {
        console.log('Stream stopped successfully');
        this.isStreaming = false;
        this.currentStreamId = null;
        this.currentHostId = null;
        this.streamWatcherCount = 0;
        this.webrtcService.closeAllPeerConnections();
        this.loadLiveStreams(); // Refresh the list
      },
      error: (error) => {
        console.error('Error stopping stream:', error);
        this.error = 'Failed to stop stream.';
        // Still reset local state
        this.isStreaming = false;
        this.currentStreamId = null;
        this.currentHostId = null;
        this.streamWatcherCount = 0;
        this.webrtcService.closeAllPeerConnections();
      }
    });
  }

  // WebRTC - Host side methods
  private handleWatcherJoined(watcherId: number): void {
    if (!this.localStream || !this.currentHostId) {
      console.error('Cannot handle watcher - missing localStream or currentHostId');
      return;
    }

    console.log('New watcher joined:', watcherId, '- Creating peer connection');

    // Create peer connection for the new watcher
    const peerConnection = this.webrtcService.createPeerConnection(
      watcherId,
      (candidate) => {
        console.log('Sending ICE candidate to watcher:', watcherId);
        // Send ICE candidate to watcher using HOST ID not stream ID
        this.webrtcService.sendIceCandidate(this.currentHostId!, watcherId, candidate);
      }
    );

    // Add local stream tracks to peer connection
    this.webrtcService.addLocalStream(watcherId, this.localStream);

    // Create and send offer to watcher using HOST ID not stream ID
    console.log('Creating offer for watcher:', watcherId);
    this.webrtcService.createOffer(watcherId, this.currentHostId);
  }

  private handleSignalAsHost(event: any): void {
    if (!event.payload) return;

    console.log('Host received SIGNAL event:', event);

    try {
      const signal = JSON.parse(event.payload);
      const senderId = event.actorId;

      console.log('Parsed signal:', signal, 'from user:', senderId);

      if (signal.type === 'answer') {
        console.log('Handling answer from watcher:', senderId);
        // Watcher sent answer, set it as remote description
        this.webrtcService.handleAnswer(senderId, signal);
      } else if (signal.candidate) {
        console.log('Handling ICE candidate from watcher:', senderId);
        // Received ICE candidate from watcher
        this.webrtcService.handleIceCandidate(senderId, signal);
      }
    } catch (error) {
      console.error('Error handling signal as host:', error);
    }
  }

  // Watcher methods
  joinStream(stream: LiveReading): void {
    if (this.isWatching || this.isStreaming) {
      this.error = 'You are already in a stream';
      return;
    }

    this.watchedStreamHostId = stream.hostId;
    this.watchedStreamHostName = stream.hostName;

    // Create peer connection for receiving host's stream
    const peerConnection = this.webrtcService.createPeerConnection(
      stream.hostId,
      (candidate) => {
        console.log('Watcher sending ICE candidate to host:', stream.hostId);
        // Send ICE candidate to host
        this.webrtcService.sendIceCandidate(stream.hostId, stream.hostId, candidate);
      },
      (remoteStream) => {
        console.log('Watcher received remote stream!');
        // Display received stream
        this.displayRemoteStream(remoteStream);
      }
    );

    const sub = this.streamService.joinStream(stream.hostId).subscribe({
      next: (event) => {
        console.log('Stream event:', event);
        
        if (event.type === 'WATCHER_JOINED') {
          this.isWatching = true;
          console.log('Successfully joined stream, waiting for offer from host');
          
          // Initialize remote video display
          setTimeout(() => {
            this.initRemoteVideoElement();
          }, 100);
        } else if (event.type === 'STREAM_STOPPED') {
          console.log('Stream ended by host');
          this.leaveStream();
          this.error = 'The stream has ended';
        } else if (event.type === 'SIGNAL') {
          // Handle WebRTC signaling from host
          this.handleSignalAsWatcher(event);
        }
      },
      error: (error) => {
        console.error('Error joining stream:', error);
        this.error = 'Failed to join stream. Please try again.';
        this.isWatching = false;
        this.watchedStreamHostId = null;
        this.watchedStreamHostName = '';
      }
    });

    this.subscriptions.add(sub);
  }

  leaveStream(): void {
    if (!this.isWatching || !this.watchedStreamHostId) return;

    const hostId = this.watchedStreamHostId;

    // Clean up remote video
    if (this.remoteVideoElement) {
      this.remoteVideoElement.srcObject = null;
      this.remoteVideoElement = null;
    }

    this.streamService.leaveStream(hostId).subscribe({
      next: () => {
        console.log('Left stream successfully');
        this.isWatching = false;
        this.watchedStreamHostId = null;
        this.watchedStreamHostName = '';
        this.webrtcService.closePeerConnection(hostId);
        this.loadLiveStreams(); // Refresh the list
      },
      error: (error) => {
        console.error('Error leaving stream:', error);
        // Still reset local state
        this.isWatching = false;
        this.watchedStreamHostId = null;
        this.watchedStreamHostName = '';
        if (hostId) {
          this.webrtcService.closePeerConnection(hostId);
        }
      }
    });
  }

  // WebRTC - Watcher side methods
  private handleSignalAsWatcher(event: any): void {
    if (!event.payload || !this.watchedStreamHostId) return;

    console.log('Watcher received SIGNAL event:', event);

    try {
      const signal = JSON.parse(event.payload);

      console.log('Parsed signal:', signal);

      if (signal.type === 'offer') {
        console.log('Handling offer from host:', this.watchedStreamHostId);
        // Host sent offer, handle it and send answer
        this.webrtcService.handleOffer(this.watchedStreamHostId, signal);
      } else if (signal.candidate) {
        console.log('Handling ICE candidate from host:', this.watchedStreamHostId);
        // Received ICE candidate from host
        this.webrtcService.handleIceCandidate(this.watchedStreamHostId, signal);
      }
    } catch (error) {
      console.error('Error handling signal as watcher:', error);
    }
  }

  private displayRemoteStream(stream: MediaStream): void {
    console.log('Displaying remote stream');
    if (this.remoteVideoElement) {
      this.remoteVideoElement.srcObject = stream;
      this.remoteVideoElement.play().catch(error => {
        console.error('Error playing remote video:', error);
      });
    } else {
      // Try again after a short delay
      setTimeout(() => {
        this.remoteVideoElement = document.getElementById('remoteVideo') as HTMLVideoElement;
        if (this.remoteVideoElement) {
          this.remoteVideoElement.srcObject = stream;
          this.remoteVideoElement.play().catch(error => {
            console.error('Error playing remote video:', error);
          });
        }
      }, 500);
    }
  }

  private initRemoteVideoElement(): void {
    this.remoteVideoElement = document.getElementById('remoteVideo') as HTMLVideoElement;
    if (this.remoteVideoElement) {
      // For now, we'll need WebRTC to actually receive video
      // This is just the DOM element setup
      console.log('Remote video element initialized');
    }
  }

  isCurrentUserStream(stream: LiveReading): boolean {
    const user = this.authState.getUserFromStorage();
    const isOwn = user?.id === stream.hostId;
    console.log('isCurrentUserStream check:', { userId: user?.id, hostId: stream.hostId, isOwn });
    return isOwn;
  }

  getReadingProgress(reading: LiveReading): number {
    if (!reading.currentPage || !reading.totalPages || reading.totalPages === 0) {
      return 0;
    }
    return Math.round((reading.currentPage / reading.totalPages) * 100);
  }

  getTimeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  }

  // Pagination
  loadNextPage(): void {
    if (!this.isLastPage) {
      this.loadUserPosts(this.currentPage + 1);
    }
  }

  loadPreviousPage(): void {
    if (this.currentPage > 0) {
      this.loadUserPosts(this.currentPage - 1);
    }
  }

  // Create Post Modal
  openCreatePostModal(): void {
    this.editingPost = null;
    this.showCreatePostModal = true;
  }

  openEditPostModal(post: HomePost): void {
    this.editingPost = post;
    this.showCreatePostModal = true;
  }

  closeCreatePostModal(): void {
    this.showCreatePostModal = false;
    this.editingPost = null;
  }

  onPostCreated(): void {
    this.loadUserPosts(0); // Reload posts from first page
  }

  // Delete Post
  deletePost(post: HomePost): void {
    if (confirm('Are you sure you want to delete this post?')) {
      this.homePostService.deletePost(post.id).subscribe({
        next: () => {
          console.log('Post deleted successfully');
          this.loadUserPosts(this.currentPage);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error deleting post:', error);
          this.error = 'Failed to delete post. You may not have permission.';
        }
      });
    }
  }

  // Check if current user is post owner
  isPostOwner(post: HomePost): boolean {
    const currentUser = this.authState.getCurrentUser();
    
    if (!currentUser || !currentUser.email) {
      return false;
    }
    
    if (!post.authorEmail) {
      return false;
    }
    
    // Compare emails (case-insensitive)
    return currentUser.email.toLowerCase() === post.authorEmail.toLowerCase();
  }

  // Image Lightbox/Preview
  openLightbox(imageUrl: string, imageName: string): void {
    this.lightboxImageUrl = imageUrl;
    this.lightboxImageName = imageName;
    this.showLightbox = true;
    // Prevent body scroll when lightbox is open
    document.body.style.overflow = 'hidden';
  }

  closeLightbox(): void {
    this.showLightbox = false;
    this.lightboxImageUrl = '';
    this.lightboxImageName = '';
    // Restore body scroll
    document.body.style.overflow = 'auto';
  }

  downloadAttachment(dataUri: string, fileName: string): void {
    const link = document.createElement('a');
    link.href = dataUri;
    link.download = fileName;
    link.click();
  }
}

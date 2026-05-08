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

export interface DtoIceServer {
  urls: string | string[];
  username?: string;
  credential?: string;
}

export interface StreamEvent {
  type: 'STREAM_STARTED' | 'WATCHER_JOINED' | 'WATCHER_LEFT' | 'STREAM_STOPPED' | 'SIGNAL';
  streamId: number;
  actorId: number;
  actorName: string;
  payload?: string;
  watcherCount?: number;
}
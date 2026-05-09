import { Component, OnInit } from '@angular/core';
import { AuthStateService } from '../../service/auth/auth-state.service';
import {
  DtoRelationResponse,
  DtoUserSearchResult,
  RelationStatus
} from '../../interfaces/relation.interface';
import { RelationService } from '../../service/relation/relation.service';
import { getApiErrorMessage } from '../../service/error/api-error.util';

@Component({
  selector: 'app-friends',
  standalone: false,
  templateUrl: './friends.component.html',
  styleUrl: './friends.component.scss'
})
export class FriendsComponent implements OnInit {
  private readonly seedSearchQuery = 'a';

  isLoggedIn = false;
  currentUserId: number | null = null;

  searchQuery = '';
  searchResults: DtoUserSearchResult[] = [];
  searchLoading = false;
  searchError = '';

  incomingRequests: DtoRelationResponse[] = [];
  incomingLoading = false;

  friends: DtoRelationResponse[] = [];
  friendsLoading = false;

  actionMessage = '';
  actionMessageType: 'success' | 'error' | '' = '';

  constructor(
    private authState: AuthStateService,
    private relationService: RelationService
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.normalizeId(this.authState.getCurrentUser()?.id);

    this.authState.user$.subscribe(user => {
      this.isLoggedIn = !!user;
      this.currentUserId = this.normalizeId(user?.id);

      if (this.isLoggedIn) {
        this.loadIncomingRequests();
        this.loadFriends();
        this.loadSeedUsers();
      } else {
        this.incomingRequests = [];
        this.friends = [];
        this.searchResults = [];
      }
    });
  }

  searchUsers(): void {
    const query = this.searchQuery.trim();
    this.searchError = '';

    if (!query) {
      this.searchResults = [];
      return;
    }

    this.searchLoading = true;
    this.relationService.searchUsers(query, 0, 20).subscribe({
      next: (res) => {
        this.searchResults = res.content;
        this.searchLoading = false;
      },
      error: (error) => {
        this.searchLoading = false;
        this.searchError = getApiErrorMessage(error, 'Failed to search users.');
      }
    });
  }

  private loadSeedUsers(): void {
    // Seed the page with discoverable users so it is not empty on first load.
    this.searchQuery = this.seedSearchQuery;
    this.searchUsers();
  }

  loadIncomingRequests(): void {
    this.incomingLoading = true;
    this.relationService.getIncomingFriendRequests(0, 10).subscribe({
      next: (res) => {
        this.incomingRequests = res.content;
        this.incomingLoading = false;
      },
      error: () => {
        this.incomingLoading = false;
      }
    });
  }

  loadFriends(): void {
    this.friendsLoading = true;
    this.relationService.getFriends(0, 10).subscribe({
      next: (res) => {
        this.friends = res.content;
        this.friendsLoading = false;
      },
      error: () => {
        this.friendsLoading = false;
      }
    });
  }

  sendFriendRequest(user: DtoUserSearchResult): void {
    this.clearActionMessage();
    this.relationService.sendFriendRequest(user.id).subscribe({
      next: () => {
        user.friendRequestStatus = 'PENDING';
        this.setSuccess('Friend request sent.');
      },
      error: (error) => {
        this.setError(getApiErrorMessage(error, 'Failed to send friend request.'));
      }
    });
  }

  acceptRequest(request: DtoRelationResponse): void {
    this.clearActionMessage();
    this.relationService.acceptFriendRequest(request.id).subscribe({
      next: () => {
        this.setSuccess('Friend request accepted.');
        this.loadIncomingRequests();
        this.loadFriends();
        this.searchUsers();
      },
      error: (error) => {
        this.setError(getApiErrorMessage(error, 'Failed to accept request.'));
      }
    });
  }

  rejectRequest(request: DtoRelationResponse): void {
    this.clearActionMessage();
    this.relationService.rejectFriendRequest(request.id).subscribe({
      next: () => {
        this.setSuccess('Friend request rejected.');
        this.loadIncomingRequests();
        this.searchUsers();
      },
      error: (error) => {
        this.setError(getApiErrorMessage(error, 'Failed to reject request.'));
      }
    });
  }

  removeFriend(relation: DtoRelationResponse): void {
    const targetUserId = this.getOtherUserId(relation);
    if (!targetUserId) {
      return;
    }

    this.clearActionMessage();
    this.relationService.removeFriend(targetUserId).subscribe({
      next: () => {
        this.setSuccess('Friend removed.');
        this.loadFriends();
        this.searchUsers();
      },
      error: (error) => {
        this.setError(getApiErrorMessage(error, 'Failed to remove friend.'));
      }
    });
  }

  toggleFollow(user: DtoUserSearchResult): void {
    this.clearActionMessage();

    if (user.isFollowing) {
      this.relationService.unfollowUser(user.id).subscribe({
        next: () => {
          user.isFollowing = false;
          this.setSuccess('Unfollowed user.');
        },
        error: (error) => {
          this.setError(getApiErrorMessage(error, 'Failed to unfollow user.'));
        }
      });
      return;
    }

    this.relationService.followUser(user.id).subscribe({
      next: () => {
        user.isFollowing = true;
        this.setSuccess('Now following user.');
      },
      error: (error) => {
        this.setError(getApiErrorMessage(error, 'Failed to follow user.'));
      }
    });
  }

  getOtherUserName(relation: DtoRelationResponse): string {
    if (!this.currentUserId) {
      return relation.requesterFullName;
    }

    return relation.requesterId === this.currentUserId
      ? relation.addresseeFullName
      : relation.requesterFullName;
  }

  getOtherUserId(relation: DtoRelationResponse): number | null {
    if (this.currentUserId) {
      return relation.requesterId === this.currentUserId
        ? relation.addresseeId
        : relation.requesterId;
    }

    // Fallback when current user id is unavailable from auth payload.
    return relation.addresseeId || relation.requesterId || null;
  }

  getChatFriendId(relation: DtoRelationResponse): number | null {
    return this.getOtherUserId(relation);
  }

  hasPendingRequest(status: RelationStatus | null): boolean {
    return status === 'PENDING';
  }

  private clearActionMessage(): void {
    this.actionMessage = '';
    this.actionMessageType = '';
  }

  private setSuccess(message: string): void {
    this.actionMessageType = 'success';
    this.actionMessage = message;
  }

  private setError(message: string): void {
    this.actionMessageType = 'error';
    this.actionMessage = message;
  }

  private normalizeId(id: unknown): number | null {
    const parsed = Number(id);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }
}

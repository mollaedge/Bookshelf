import { PageResponse } from './page.interface';

export type RelationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';
export type RelationType = 'FRIEND_REQUEST' | 'FOLLOW';

export interface DtoRelationResponse {
  id: number;
  requesterId: number;
  requesterFullName: string;
  addresseeId: number;
  addresseeFullName: string;
  relationType: RelationType;
  status: RelationStatus;
  createdAt: string;
}

export interface DtoUserSearchResult {
  id: number;
  firstname: string;
  lastname: string;
  fullName: string;
  email: string;
  bio: string;
  location: string;
  hasProfilePic: boolean;
  isFriend: boolean;
  friendRequestStatus: RelationStatus | null;
  pendingFriendRequestId: number | null;
  isFollowing: boolean;
}

export interface DtoFriendPageResponse {
  userId: number;
  firstname: string;
  lastname: string;
  fullName: string;
  email: string;
  bio: string;
  location: string;
  hasProfilePic: boolean;
  hasWallpaper: boolean;
  dateOfBirth: string;
  friendCount: number;
  followersCount: number;
  followingCount: number;
  isFriend: boolean;
  friendRequestStatus: RelationStatus | null;
  pendingFriendRequestId: number | null;
  isFollowing: boolean;
  isFollowedByTarget: boolean;
}

export type RelationPageResponse = PageResponse<DtoRelationResponse>;
export type UserSearchPageResponse = PageResponse<DtoUserSearchResult>;

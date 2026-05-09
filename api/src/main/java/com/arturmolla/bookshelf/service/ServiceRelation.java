package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoFriendPageResponse;
import com.arturmolla.bookshelf.model.dto.DtoRelationResponse;
import com.arturmolla.bookshelf.model.dto.DtoUserSearchResult;
import com.arturmolla.bookshelf.model.entity.EntityUserRelation;
import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.enums.RelationStatus;
import com.arturmolla.bookshelf.model.enums.RelationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.repository.RepositoryUserRelation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceRelation {

    private final RepositoryUserRelation relationRepository;
    private final RepositoryUser userRepository;
    private final ServiceNotification notificationService;
    private final ServiceFileStorage fileStorage;

    // ─────────────────────────────────────────────────────────────────────────
    // Friend requests
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a friend request from the authenticated user to the target user.
     *
     * @throws IllegalArgumentException if a relation already exists or the user tries to add themselves.
     */
    public DtoRelationResponse sendFriendRequest(Long targetUserId, Authentication connectedUser) {
        var requester = (User) connectedUser.getPrincipal();
        if (requester.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        User addressee = findUser(targetUserId);

        if (relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                requester.getId(), targetUserId, RelationType.FRIEND_REQUEST)
                || relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                targetUserId, requester.getId(), RelationType.FRIEND_REQUEST)) {
            throw new IllegalArgumentException("A friend request already exists between these users.");
        }

        EntityUserRelation relation = EntityUserRelation.builder()
                .requester(requester)
                .addressee(addressee)
                .relationType(RelationType.FRIEND_REQUEST)
                .status(RelationStatus.PENDING)
                .build();

        relation = relationRepository.save(relation);

        notificationService.notify(
                addressee,
                requester,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "New friend request",
                requester.getFullName() + " sent you a friend request.",
                relation.getId(),
                "FRIEND_REQUEST"
        );

        log.info("Friend request sent from userId={} to userId={}", requester.getId(), targetUserId);
        return toRelationResponse(relation);
    }

    /**
     * Accepts a pending friend request. Only the addressee may accept.
     */
    public DtoRelationResponse acceptFriendRequest(Long requestId, Authentication connectedUser) {
        var addressee = (User) connectedUser.getPrincipal();
        EntityUserRelation relation = findRelation(requestId);
        validateAddresseeOwnership(relation, addressee);
        validatePendingStatus(relation);

        relation.setStatus(RelationStatus.ACCEPTED);
        relation = relationRepository.save(relation);

        notificationService.notify(
                relation.getRequester(),
                addressee,
                NotificationType.FRIEND_REQUEST_ACCEPTED,
                "Friend request accepted",
                addressee.getFullName() + " accepted your friend request.",
                relation.getId(),
                "FRIEND_REQUEST"
        );

        log.info("Friend request id={} accepted by userId={}", requestId, addressee.getId());
        return toRelationResponse(relation);
    }

    /**
     * Rejects a pending friend request. Only the addressee may reject.
     */
    public DtoRelationResponse rejectFriendRequest(Long requestId, Authentication connectedUser) {
        var addressee = (User) connectedUser.getPrincipal();
        EntityUserRelation relation = findRelation(requestId);
        validateAddresseeOwnership(relation, addressee);
        validatePendingStatus(relation);

        relation.setStatus(RelationStatus.REJECTED);
        relation = relationRepository.save(relation);

        notificationService.notify(
                relation.getRequester(),
                addressee,
                NotificationType.FRIEND_REQUEST_REJECTED,
                "Friend request declined",
                addressee.getFullName() + " declined your friend request.",
                relation.getId(),
                "FRIEND_REQUEST"
        );

        log.info("Friend request id={} rejected by userId={}", requestId, addressee.getId());
        return toRelationResponse(relation);
    }

    /**
     * Removes an existing friendship. Either party may remove the other.
     * Also handles cancelling a pending outgoing request by the requester.
     */
    public void removeFriend(Long targetUserId, Authentication connectedUser) {
        var currentUser = (User) connectedUser.getPrincipal();

        // Try requester → targetUser direction first
        EntityUserRelation relation = relationRepository
                .findByRequesterIdAndAddresseeIdAndRelationType(
                        currentUser.getId(), targetUserId, RelationType.FRIEND_REQUEST)
                .orElseGet(() ->
                        // Try reverse direction
                        relationRepository.findByRequesterIdAndAddresseeIdAndRelationType(
                                targetUserId, currentUser.getId(), RelationType.FRIEND_REQUEST)
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "No friend relation found with userId=" + targetUserId))
                );

        relationRepository.delete(relation);
        log.info("Friendship removed between userId={} and userId={}", currentUser.getId(), targetUserId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Follows
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Follows a user. The follow is one-directional; following back creates a second record.
     */
    public DtoRelationResponse followUser(Long targetUserId, Authentication connectedUser) {
        var follower = (User) connectedUser.getPrincipal();
        if (follower.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        User followed = findUser(targetUserId);

        if (relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                follower.getId(), targetUserId, RelationType.FOLLOW)) {
            throw new IllegalArgumentException("You are already following this user.");
        }

        EntityUserRelation relation = EntityUserRelation.builder()
                .requester(follower)
                .addressee(followed)
                .relationType(RelationType.FOLLOW)
                .status(RelationStatus.ACCEPTED)
                .build();

        relation = relationRepository.save(relation);

        notificationService.notify(
                followed,
                follower,
                NotificationType.FOLLOWED,
                "New follower",
                follower.getFullName() + " started following you.",
                relation.getId(),
                "FOLLOW"
        );

        log.info("userId={} is now following userId={}", follower.getId(), targetUserId);
        return toRelationResponse(relation);
    }

    /**
     * Unfollows a user the authenticated user is currently following.
     */
    public void unfollowUser(Long targetUserId, Authentication connectedUser) {
        var follower = (User) connectedUser.getPrincipal();
        EntityUserRelation relation = relationRepository
                .findByRequesterIdAndAddresseeIdAndRelationType(
                        follower.getId(), targetUserId, RelationType.FOLLOW)
                .orElseThrow(() -> new EntityNotFoundException(
                        "You are not following userId=" + targetUserId));

        relationRepository.delete(relation);
        log.info("userId={} unfollowed userId={}", follower.getId(), targetUserId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listings
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns all accepted friends of the authenticated user, paged. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoRelationResponse> getMyFriends(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityUserRelation> result = relationRepository.findFriends(
                user.getId(), PageRequest.of(page, size));
        return toPageResponse(result);
    }

    /** Returns incoming pending friend requests for the authenticated user. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoRelationResponse> getIncomingFriendRequests(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityUserRelation> result = relationRepository
                .findByAddresseeIdAndRelationTypeAndStatus(
                        user.getId(), RelationType.FRIEND_REQUEST, RelationStatus.PENDING,
                        PageRequest.of(page, size));
        return toPageResponse(result);
    }

    /** Returns outgoing pending friend requests sent by the authenticated user. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoRelationResponse> getOutgoingFriendRequests(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityUserRelation> result = relationRepository
                .findByRequesterIdAndRelationTypeAndStatus(
                        user.getId(), RelationType.FRIEND_REQUEST, RelationStatus.PENDING,
                        PageRequest.of(page, size));
        return toPageResponse(result);
    }

    /** Returns users that the authenticated user is following. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoRelationResponse> getFollowing(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityUserRelation> result = relationRepository
                .findByRequesterIdAndRelationType(user.getId(), RelationType.FOLLOW, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    /** Returns users who follow the authenticated user. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoRelationResponse> getFollowers(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityUserRelation> result = relationRepository
                .findByAddresseeIdAndRelationType(user.getId(), RelationType.FOLLOW, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Friend / User page view
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the public profile page of a target user enriched with the
     * authenticated user's relation context (friend status, follow status, counts).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DtoFriendPageResponse viewUserPage(Long targetUserId, Authentication connectedUser) {
        var currentUser = (User) connectedUser.getPrincipal();
        User target = findUser(targetUserId);

        // Social counts
        long friendCount   = relationRepository.countFriends(targetUserId);
        long followersCount = relationRepository.countByAddresseeIdAndRelationType(targetUserId, RelationType.FOLLOW);
        long followingCount = relationRepository.countByRequesterIdAndRelationType(targetUserId, RelationType.FOLLOW);

        // Relation context: current user → target
        boolean isFriend = false;
        RelationStatus friendRequestStatus = null;
        Long pendingFriendRequestId = null;
        boolean isFollowing = false;
        boolean isFollowedByTarget = false;

        // Check friendship (request may be in either direction)
        var frForward = relationRepository.findByRequesterIdAndAddresseeIdAndRelationType(
                currentUser.getId(), targetUserId, RelationType.FRIEND_REQUEST);
        var frReverse = relationRepository.findByRequesterIdAndAddresseeIdAndRelationType(
                targetUserId, currentUser.getId(), RelationType.FRIEND_REQUEST);

        if (frForward.isPresent()) {
            friendRequestStatus = frForward.get().getStatus();
            isFriend = RelationStatus.ACCEPTED.equals(friendRequestStatus);
            if (RelationStatus.PENDING.equals(friendRequestStatus)) {
                pendingFriendRequestId = frForward.get().getId();
            }
        } else if (frReverse.isPresent()) {
            friendRequestStatus = frReverse.get().getStatus();
            isFriend = RelationStatus.ACCEPTED.equals(friendRequestStatus);
            if (RelationStatus.PENDING.equals(friendRequestStatus)) {
                pendingFriendRequestId = frReverse.get().getId();
            }
        }

        isFollowing = relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                currentUser.getId(), targetUserId, RelationType.FOLLOW);
        isFollowedByTarget = relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                targetUserId, currentUser.getId(), RelationType.FOLLOW);

        return DtoFriendPageResponse.builder()
                .userId(target.getId())
                .firstname(target.getFirstname())
                .lastname(target.getLastname())
                .fullName(target.getFullName())
                .email(target.getEmail())
                .bio(target.getBio())
                .location(target.getLocation())
                .dateOfBirth(target.getDateOfBirth())
                .hasProfilePic(fileStorage.hasProfilePic(target.getId()))
                .hasWallpaper(fileStorage.hasWallpaper(target.getId()))
                .friendCount(friendCount)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .isFriend(isFriend)
                .friendRequestStatus(friendRequestStatus)
                .pendingFriendRequestId(pendingFriendRequestId)
                .isFollowing(isFollowing)
                .isFollowedByTarget(isFollowedByTarget)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches for users by name or e-mail.
     * The authenticated user is excluded from the results.
     * Each result is enriched with the caller's relation context.
     *
     * @param query         partial name or e-mail (case-insensitive)
     * @param page          0-based page index
     * @param size          page size
     * @param connectedUser currently authenticated user
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<DtoUserSearchResult> searchUsers(String query, int page, int size,
                                                         Authentication connectedUser) {
        var currentUser = (User) connectedUser.getPrincipal();
        Page<User> result = userRepository.searchUsers(
                query, currentUser.getId(), PageRequest.of(page, size));

        List<DtoUserSearchResult> content = result.getContent()
                .stream()
                .map(u -> toUserSearchResult(u, currentUser))
                .toList();

        return PageResponse.<DtoUserSearchResult>builder()
                .content(content)
                .number(result.getNumber())
                .size(result.getSize())
                .totalElement(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    private EntityUserRelation findRelation(Long relationId) {
        return relationRepository.findById(relationId)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found with id: " + relationId));
    }

    private void validateAddresseeOwnership(EntityUserRelation relation, User user) {
        if (!relation.getAddressee().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not the recipient of this friend request.");
        }
    }

    private void validatePendingStatus(EntityUserRelation relation) {
        if (!RelationStatus.PENDING.equals(relation.getStatus())) {
            throw new IllegalStateException("This friend request is no longer pending.");
        }
    }

    private DtoRelationResponse toRelationResponse(EntityUserRelation r) {
        return DtoRelationResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequester().getId())
                .requesterFullName(r.getRequester().getFullName())
                .addresseeId(r.getAddressee().getId())
                .addresseeFullName(r.getAddressee().getFullName())
                .relationType(r.getRelationType())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private PageResponse<DtoRelationResponse> toPageResponse(Page<EntityUserRelation> page) {
        List<DtoRelationResponse> content = page.getContent()
                .stream()
                .map(this::toRelationResponse)
                .toList();

        return PageResponse.<DtoRelationResponse>builder()
                .content(content)
                .number(page.getNumber())
                .size(page.getSize())
                .totalElement(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private DtoUserSearchResult toUserSearchResult(User target, User currentUser) {
        Long currentId = currentUser.getId();
        Long targetId  = target.getId();

        // Check friendship (request in either direction)
        boolean isFriend = false;
        RelationStatus friendRequestStatus = null;
        Long pendingFriendRequestId = null;

        var frForward = relationRepository.findByRequesterIdAndAddresseeIdAndRelationType(
                currentId, targetId, RelationType.FRIEND_REQUEST);
        var frReverse = relationRepository.findByRequesterIdAndAddresseeIdAndRelationType(
                targetId, currentId, RelationType.FRIEND_REQUEST);

        if (frForward.isPresent()) {
            friendRequestStatus = frForward.get().getStatus();
            isFriend = RelationStatus.ACCEPTED.equals(friendRequestStatus);
            if (RelationStatus.PENDING.equals(friendRequestStatus)) {
                pendingFriendRequestId = frForward.get().getId();
            }
        } else if (frReverse.isPresent()) {
            friendRequestStatus = frReverse.get().getStatus();
            isFriend = RelationStatus.ACCEPTED.equals(friendRequestStatus);
            if (RelationStatus.PENDING.equals(friendRequestStatus)) {
                pendingFriendRequestId = frReverse.get().getId();
            }
        }

        boolean isFollowing = relationRepository.existsByRequesterIdAndAddresseeIdAndRelationType(
                currentId, targetId, RelationType.FOLLOW);

        return DtoUserSearchResult.builder()
                .id(target.getId())
                .firstname(target.getFirstname())
                .lastname(target.getLastname())
                .fullName(target.getFullName())
                .email(target.getEmail())
                .bio(target.getBio())
                .location(target.getLocation())
                .hasProfilePic(fileStorage.hasProfilePic(target.getId()))
                .isFriend(isFriend)
                .friendRequestStatus(friendRequestStatus)
                .pendingFriendRequestId(pendingFriendRequestId)
                .isFollowing(isFollowing)
                .build();
    }
}


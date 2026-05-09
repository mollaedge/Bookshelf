package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoFriendPageResponse;
import com.arturmolla.bookshelf.model.dto.DtoRelationResponse;
import com.arturmolla.bookshelf.model.dto.DtoUserSearchResult;
import com.arturmolla.bookshelf.service.ServiceRelation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("relations")
@RequiredArgsConstructor
@Tag(name = "Relations", description = "Friend requests, friendships, follows and user page")
public class ControllerRelation {

    private final ServiceRelation relationService;

    // ─────────────────────────────────────────────────────────────────────────
    // Friend requests
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/friend-request/{targetUserId}")
    @Operation(summary = "Send a friend request to another user")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<DtoRelationResponse> sendFriendRequest(
            @Parameter(description = "ID of the user to send the request to")
            @PathVariable Long targetUserId,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.sendFriendRequest(targetUserId, connectedUser));
    }

    @PutMapping("/friend-request/{requestId}/accept")
    @Operation(summary = "Accept an incoming friend request")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<DtoRelationResponse> acceptFriendRequest(
            @Parameter(description = "ID of the friend request to accept")
            @PathVariable Long requestId,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.acceptFriendRequest(requestId, connectedUser));
    }

    @PutMapping("/friend-request/{requestId}/reject")
    @Operation(summary = "Reject an incoming friend request")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<DtoRelationResponse> rejectFriendRequest(
            @Parameter(description = "ID of the friend request to reject")
            @PathVariable Long requestId,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.rejectFriendRequest(requestId, connectedUser));
    }

    @GetMapping("/friend-requests/incoming")
    @Operation(summary = "Get incoming pending friend requests")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoRelationResponse>> getIncomingFriendRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.getIncomingFriendRequests(page, size, connectedUser));
    }

    @GetMapping("/friend-requests/outgoing")
    @Operation(summary = "Get outgoing pending friend requests")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoRelationResponse>> getOutgoingFriendRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.getOutgoingFriendRequests(page, size, connectedUser));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Friendship management
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/friends")
    @Operation(summary = "Get the current user's friends list")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoRelationResponse>> getMyFriends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.getMyFriends(page, size, connectedUser));
    }

    @DeleteMapping("/friends/{targetUserId}")
    @Operation(summary = "Remove a friend or cancel a pending friend request")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<Void> removeFriend(
            @Parameter(description = "ID of the friend to remove")
            @PathVariable Long targetUserId,
            Authentication connectedUser) {
        relationService.removeFriend(targetUserId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Follow / Unfollow
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/follow/{targetUserId}")
    @Operation(summary = "Follow a user")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<DtoRelationResponse> followUser(
            @Parameter(description = "ID of the user to follow")
            @PathVariable Long targetUserId,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.followUser(targetUserId, connectedUser));
    }

    @DeleteMapping("/follow/{targetUserId}")
    @Operation(summary = "Unfollow a user")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<Void> unfollowUser(
            @Parameter(description = "ID of the user to unfollow")
            @PathVariable Long targetUserId,
            Authentication connectedUser) {
        relationService.unfollowUser(targetUserId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following")
    @Operation(summary = "Get users the current user is following")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoRelationResponse>> getFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.getFollowing(page, size, connectedUser));
    }

    @GetMapping("/followers")
    @Operation(summary = "Get users who follow the current user")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoRelationResponse>> getFollowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.getFollowers(page, size, connectedUser));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User search
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/users/search")
    @Operation(
            summary = "Search for users by name or e-mail",
            description = "Case-insensitive partial match on first name, last name, full name, or e-mail. " +
                    "The authenticated user is excluded from results. " +
                    "Each result includes the caller's relation context (friend status, follow status)."
    )
    @RateLimit(capacity = 30, refillTokens = 30, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoUserSearchResult>> searchUsers(
            @Parameter(description = "Partial name or e-mail to search for", required = true)
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.searchUsers(query, page, size, connectedUser));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User / Friend page
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}")
    @Operation(summary = "View another user's profile page with social context")
    @RateLimit(capacity = 30, refillTokens = 30, refillDurationMinutes = 1)
    public ResponseEntity<DtoFriendPageResponse> viewUserPage(
            @Parameter(description = "ID of the user whose page to view")
            @PathVariable Long userId,
            Authentication connectedUser) {
        return ResponseEntity.ok(relationService.viewUserPage(userId, connectedUser));
    }
}


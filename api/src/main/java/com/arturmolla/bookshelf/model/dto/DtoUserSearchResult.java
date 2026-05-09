package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.RelationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight user card returned from user search results.
 * Includes just enough social context for the caller to render action buttons.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoUserSearchResult {

    // ── User info ──────────────────────────────────────────────────
    private Long id;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private String bio;
    private String location;
    private boolean hasProfilePic;

    // ── Relation context (current-user → found user) ───────────────
    /** True if the two users are already friends (accepted). */
    private boolean isFriend;

    /**
     * Status of a friend request between the two users, if any.
     * Null when no request has ever been sent.
     */
    private RelationStatus friendRequestStatus;

    /**
     * ID of a pending friend request (for accept/reject actions).
     * Null when no pending request exists.
     */
    private Long pendingFriendRequestId;

    /** True if the current user is following this user. */
    private boolean isFollowing;
}


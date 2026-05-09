package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.RelationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Full profile of another user as seen from the perspective of the
 * currently authenticated user (includes mutual-relation context).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoFriendPageResponse {

    // ── Target user info ──────────────────────────────────────────
    private Long userId;
    private String firstname;
    private String lastname;
    private String fullName;
    private String email;
    private String bio;
    private String location;
    private boolean hasProfilePic;
    private boolean hasWallpaper;
    private LocalDate dateOfBirth;

    // ── Social counts ─────────────────────────────────────────────
    private long friendCount;
    private long followersCount;
    private long followingCount;

    // ── Relation context (from authenticated user's POV) ──────────
    /** Whether the authenticated user and the target are friends. */
    private boolean isFriend;

    /** If a friend request exists between the two, its status. */
    private RelationStatus friendRequestStatus;

    /**
     * ID of the pending friend request (useful for accept/reject actions).
     * Null when there is no pending request.
     */
    private Long pendingFriendRequestId;

    /** Whether the authenticated user is following the target user. */
    private boolean isFollowing;

    /** Whether the target user is following the authenticated user. */
    private boolean isFollowedByTarget;
}


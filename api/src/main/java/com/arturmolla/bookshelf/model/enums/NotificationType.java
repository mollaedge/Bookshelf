package com.arturmolla.bookshelf.model.enums;

public enum NotificationType {

    // ── Post events ──────────────────────────────────────────
    POST_LIKED,
    POST_COMMENTED,
    POST_SHARED,

    // ── Comment events ────────────────────────────────────────
    TAGGED,          // mentioned / tagged in a post or comment

    // ── Book events ───────────────────────────────────────────
    FEEDBACK_RECEIVED,

    // ── App-feedback events ───────────────────────────────────
    FEEDBACK_UPVOTED,
    FEEDBACK_COMMENTED,
    BOOK_BORROW_REQUESTED,
    BOOK_BORROW_APPROVED,
    BOOK_BORROW_REJECTED,
    BOOK_RETURNED,
    BOOK_RETURN_APPROVED
}


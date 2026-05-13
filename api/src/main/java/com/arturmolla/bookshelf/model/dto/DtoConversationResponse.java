package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Summary of a conversation shown in the inbox list.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoConversationResponse {

    private Long conversationId;

    /** The other participant (not the caller). */
    private Long friendId;
    private String friendName;

    /** Preview text of the most-recent message, or {@code null} if no messages yet. */
    private String lastMessagePreview;
    private Instant lastMessageAt;

    /** Count of unread messages sent by the friend to the caller. */
    private long unreadCount;
}


package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight snapshot of the message being replied to, embedded inside
 * {@link DtoMessageResponse}.  Uses a content snippet (max 200 chars) so the
 * payload stays small regardless of the original message length.
 * <p>
 * {@code null} when the message is a top-level message (not a reply).
 * {@code senderId} / {@code senderName} may reflect a deleted user if the
 * original sender was removed; {@code content} is {@code null} when the
 * original message itself was deleted (ON DELETE SET NULL on the FK).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoMessageReplySnippet {

    /** ID of the original message that was replied to. */
    private Long id;

    /** Sender of the original message. */
    private Long senderId;
    private String senderName;

    /**
     * Truncated content of the original message (max 200 chars).
     * {@code null} if the original message has since been deleted.
     */
    private String contentSnippet;
}


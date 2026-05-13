package com.arturmolla.bookshelf.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Returned for every message — both in paginated history and in real-time SSE events.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DtoMessageResponse {

    private Long id;
    private Long conversationId;

    /** ID of the user who sent the message. */
    private Long senderId;
    private String senderName;

    private String content;

    /**
     * Snippet of the message being replied to, or {@code null} if this is a top-level message.
     */
    private DtoMessageReplySnippet replyTo;

    private boolean read;
    private Instant createdAt;

    private String mediaType;
    private String mediaName;
    private Long mediaSize;
    private boolean hasMedia;
    private byte[] mediaData;
}
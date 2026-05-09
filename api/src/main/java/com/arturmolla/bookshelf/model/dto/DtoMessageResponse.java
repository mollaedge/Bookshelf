package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Returned for every message — both in paginated history and in real-time SSE events.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoMessageResponse {

    private Long id;
    private Long conversationId;

    /** ID of the user who sent the message. */
    private Long senderId;
    private String senderName;

    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}


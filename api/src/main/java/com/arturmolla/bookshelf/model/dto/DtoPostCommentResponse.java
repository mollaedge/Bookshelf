package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DtoPostCommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorName,
        String authorEmail,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {
}


package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DtoHomePostResponse(
        Long id,
        String title,
        String content,
        String authorName,
        Long authorId,
        String authorEmail,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        List<DtoAttachmentResponse> attachments
) {
}


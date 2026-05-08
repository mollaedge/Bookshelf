package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DtoNotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Long referenceId,
        String referenceType,
        Long actorId,
        String actorName,
        LocalDateTime createdAt
) {
}


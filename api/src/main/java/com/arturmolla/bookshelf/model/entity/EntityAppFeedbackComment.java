package com.arturmolla.bookshelf.model.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntityAppFeedbackComment {

    private Long authorId;
    private String authorName;
    private String message;
    private LocalDateTime createdAt;
}


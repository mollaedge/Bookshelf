package com.arturmolla.bookshelf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoComment {

    private Long authorId;
    private String authorName;
    private String message;
    private LocalDateTime createdAt;
    /** True when this comment was posted by the system (e.g. an admin status-change note). */
    private boolean system;
}


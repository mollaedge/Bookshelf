package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DtoPostCommentRequest(
        @NotBlank(message = "Comment content must not be blank")
        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        String content,

        /** Optional list of user IDs to tag in this comment. Each tagged user receives a TAGGED notification. */
        List<Long> taggedUserIds
) {
}


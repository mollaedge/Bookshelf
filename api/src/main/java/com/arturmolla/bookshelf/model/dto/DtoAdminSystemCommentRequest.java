package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DtoAdminSystemCommentRequest(
        @NotNull(message = "Message is required")
        @NotBlank(message = "Message must not be blank")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String message
) {
}


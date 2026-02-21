package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppFeedbackRequest(
        @NotNull(message = "Title is required")
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotNull(message = "Description is required")
        @NotBlank(message = "Description must not be blank")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {
}


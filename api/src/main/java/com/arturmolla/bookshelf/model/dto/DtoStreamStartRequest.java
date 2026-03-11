package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DtoStreamStartRequest(

        @NotBlank(message = "Stream title must not be blank")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title
) {
}


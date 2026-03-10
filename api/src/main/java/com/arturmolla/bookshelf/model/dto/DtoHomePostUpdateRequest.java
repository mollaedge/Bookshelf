package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.Size;

public record DtoHomePostUpdateRequest(

        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String content
) {
}


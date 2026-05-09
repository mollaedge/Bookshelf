package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for sending a new direct message.
 */
public record DtoMessageRequest(

        @NotBlank(message = "Message content must not be blank")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        String content
) {}


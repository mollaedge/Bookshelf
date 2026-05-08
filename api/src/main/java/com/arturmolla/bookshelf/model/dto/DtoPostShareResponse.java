package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

@Builder
public record DtoPostShareResponse(
        long shareCount,
        String shareUrl
) {
}


package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

@Builder
public record DtoPostLikeResponse(
        long likeCount,
        boolean likedByCurrentUser
) {
}


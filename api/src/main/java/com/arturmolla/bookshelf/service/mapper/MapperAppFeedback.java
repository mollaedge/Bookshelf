package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedbackComment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MapperAppFeedback {

    public EntityAppFeedback toEntity(AppFeedbackRequest request) {
        return EntityAppFeedback.builder()
                .title(request.title())
                .description(request.description())
                .build();
    }

    public AppFeedbackDto toDto(EntityAppFeedback entity, Long currentUserId) {
        return AppFeedbackDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .upvoteCount(entity.getUpvoteCount())
                .upvotedByCurrentUser(currentUserId != null && entity.getUpvotedBy().contains(currentUserId))
                .ownFeedback(currentUserId != null && currentUserId.equals(entity.getCreatedBy()))
                .age(entity.getAge())
                .createdDate(entity.getCreatedDate())
                .createdBy(entity.getCreatedBy())
                .comments(mapComments(entity))
                .build();
    }

    private List<CommentDto> mapComments(EntityAppFeedback entity) {
        if (entity.getComments() == null) return Collections.emptyList();
        return entity.getComments().stream()
                .map(this::toCommentDto)
                .toList();
    }

    private CommentDto toCommentDto(EntityAppFeedbackComment comment) {
        return CommentDto.builder()
                .authorId(comment.getAuthorId())
                .fullName(comment.getAuthorName())
                .message(comment.getMessage())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}


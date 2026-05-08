package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoAttachmentResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostResponse;
import com.arturmolla.bookshelf.model.dto.DtoPostCommentResponse;
import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import com.arturmolla.bookshelf.model.entity.EntityPostAttachment;
import com.arturmolla.bookshelf.model.entity.EntityPostComment;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
public class MapperHomePost {

    /**
     * Maps an entity to a response DTO with social counts.
     *
     * @param entity             the post entity
     * @param likeCount          total number of likes
     * @param commentCount       total number of comments
     * @param shareCount         total number of shares
     * @param likedByCurrentUser whether the requesting user already liked this post
     */
    public DtoHomePostResponse toResponse(EntityHomePost entity,
                                          long likeCount,
                                          long commentCount,
                                          long shareCount,
                                          boolean likedByCurrentUser) {
        List<DtoAttachmentResponse> attachments = entity.getAttachments() == null
                ? Collections.emptyList()
                : entity.getAttachments().stream()
                  .map(this::toAttachmentResponse)
                  .toList();

        return DtoHomePostResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getFullName() : null)
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorEmail(entity.getAuthor() != null ? entity.getAuthor().getEmail() : null)
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .attachments(attachments)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .shareCount(shareCount)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }

    /** Convenience overload with zeroed social counts (e.g. for internal use). */
    public DtoHomePostResponse toResponse(EntityHomePost entity) {
        return toResponse(entity, 0, 0, 0, false);
    }

    public DtoPostCommentResponse toCommentResponse(EntityPostComment comment) {
        return DtoPostCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorName(comment.getAuthor() != null ? comment.getAuthor().getFullName() : null)
                .authorEmail(comment.getAuthor() != null ? comment.getAuthor().getEmail() : null)
                .createdDate(comment.getCreatedDate())
                .lastModifiedDate(comment.getLastModifiedDate())
                .build();
    }

    public DtoAttachmentResponse toAttachmentResponse(EntityPostAttachment attachment) {        String dataUri = buildDataUri(attachment.getContentType(), attachment.getData());
        return new DtoAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                dataUri
        );
    }

    /**
     * Encodes raw bytes as a Base64 data URI ready for direct use in
     * HTML {@code src} / {@code href} attributes.
     *
     * <p>Result format: {@code data:<contentType>;base64,<encoded>}</p>
     *
     * @param contentType MIME type (e.g. {@code image/jpeg}, {@code application/pdf})
     * @param data        raw file bytes
     * @return Base64 data URI, or {@code null} if data is null/empty
     */
    private String buildDataUri(String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        String mime = (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";
        String encoded = Base64.getEncoder().encodeToString(data);
        return "data:" + mime + ";base64," + encoded;
    }
}


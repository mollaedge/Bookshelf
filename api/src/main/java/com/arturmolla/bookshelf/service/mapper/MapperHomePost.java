package com.arturmolla.bookshelf.service.mapper;

import com.arturmolla.bookshelf.model.dto.DtoAttachmentResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostResponse;
import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import com.arturmolla.bookshelf.model.entity.EntityPostAttachment;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
public class MapperHomePost {

    public DtoHomePostResponse toResponse(EntityHomePost entity) {
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
                .build();
    }

    public DtoAttachmentResponse toAttachmentResponse(EntityPostAttachment attachment) {
        String dataUri = buildDataUri(attachment.getContentType(), attachment.getData());
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


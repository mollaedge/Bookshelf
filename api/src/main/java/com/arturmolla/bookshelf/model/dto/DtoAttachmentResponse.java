package com.arturmolla.bookshelf.model.dto;

import java.time.LocalDateTime;

/**
 * Metadata + inline Base64 data URI for a post attachment.
 * <p>
 * {@code dataUri} is formatted as {@code data:<contentType>;base64,<data>}
 * so the front-end can use it directly:
 * <ul>
 *   <li>Images  → {@code <img src="${dataUri}">}</li>
 *   <li>PDFs    → {@code <iframe src="${dataUri}">} or {@code <embed src="${dataUri}">}</li>
 *   <li>Others  → create a temporary anchor with {@code href="${dataUri}" download="${fileName}"}
 *                 and click it programmatically</li>
 * </ul>
 */
public record DtoAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt,
        // Ready-to-use Base64 data URI — use as src / href directly in the FE.
        // Format: "data:<contentType>;base64,<encoded>"
        String dataUri
) {
}


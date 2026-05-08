package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostRequest;
import com.arturmolla.bookshelf.model.dto.DtoHomePostResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoPostCommentRequest;
import com.arturmolla.bookshelf.model.dto.DtoPostCommentResponse;
import com.arturmolla.bookshelf.model.dto.DtoPostLikeResponse;
import com.arturmolla.bookshelf.model.dto.DtoPostShareResponse;
import com.arturmolla.bookshelf.model.entity.EntityPostAttachment;
import com.arturmolla.bookshelf.service.ServiceHomePost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("home/posts")
@Tag(name = "Home Feed")
public class ControllerHome {

    private final ServiceHomePost serviceHomePost;

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Create a new post with optional file attachments (images, PDFs, etc.).
     * <p>
     * Send as {@code multipart/form-data}:
     * <ul>
     *   <li>{@code post}  – JSON part: {@link DtoHomePostRequest}</li>
     *   <li>{@code files} – zero or more binary files</li>
     * </ul>
     * <p>
     * Every attachment in the response contains a ready-to-use {@code dataUri}
     * (e.g. {@code data:image/jpeg;base64,...}) that the FE can assign directly
     * to an {@code <img src>}, {@code <iframe src>}, or a download anchor.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new post with optional attachments")
    public ResponseEntity<Long> createPost(
            @Valid @RequestPart("post") DtoHomePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.createPost(request, files, connectedUser));
    }

    // =========================================================================
    // READ
    // =========================================================================

    /**
     * Get a single post by its ID.
     * Attachments include an inline {@code dataUri} for immediate rendering.
     */
    @GetMapping("/{post-id}")
    @Operation(summary = "Get a post by ID (includes like/comment/share counts)")
    public ResponseEntity<DtoHomePostResponse> getPostById(
            @PathVariable("post-id") Long postId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.getPostById(postId, connectedUser));
    }

    /**
     * Get all posts ordered by date descending (newest first), paged.
     * Each post's attachments include an inline {@code dataUri}.
     */
    @GetMapping
    @Operation(summary = "Get all posts ordered by date (newest first)")
    public ResponseEntity<PageResponse<DtoHomePostResponse>> getAllPosts(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.getAllPosts(page, size, connectedUser));
    }

    /**
     * Get the authenticated user's posts, newest first.
     */
    @GetMapping("/my")
    @Operation(summary = "Get my posts ordered by date (newest first)")
    public ResponseEntity<PageResponse<DtoHomePostResponse>> getMyPosts(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "15", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.getMyPosts(page, size, connectedUser));
    }

    /**
     * Stream the raw bytes of an attachment as its native MIME type.
     * <p>
     * Prefer the {@code dataUri} field embedded in the post response for
     * small-to-medium files. Use this endpoint for:
     * <ul>
     *   <li>Direct browser links / {@code window.open()}</li>
     *   <li>Very large files where a streaming response is better than Base64</li>
     * </ul>
     * Responses are cached by the browser for 7 days using an {@code ETag}
     * derived from the attachment id, so repeated views are free.
     */
    @GetMapping("/{post-id}/attachments/{attachment-id}")
    @Operation(summary = "Stream a post attachment (browser-cacheable)")
    public ResponseEntity<byte[]> getAttachment(
            @PathVariable("post-id") Long postId,
            @PathVariable("attachment-id") Long attachmentId
    ) {
        EntityPostAttachment attachment = serviceHomePost.getAttachmentEntity(postId, attachmentId);

        String etag = "\"" + HexFormat.of().toHexDigits(attachmentId) + "\"";
        MediaType mediaType = attachment.getContentType() != null
                ? MediaType.parseMediaType(attachment.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        // PDFs and images: render inline; anything else: force download
        String disposition = isInlineType(mediaType)
                ? "inline; filename=\"" + attachment.getFileName() + "\""
                : "attachment; filename=\"" + attachment.getFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(attachment.getData());
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Update a post's title and/or content. Only the post author can do this.
     */
    @PutMapping("/{post-id}")
    @Operation(summary = "Update post title / content (author only)")
    public ResponseEntity<DtoHomePostResponse> updatePost(
            @PathVariable("post-id") Long postId,
            @Valid @RequestBody DtoHomePostUpdateRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.updatePost(postId, request, connectedUser));
    }

    /**
     * Add more attachments to an existing post. Only the post author can do this.
     * Send as {@code multipart/form-data} with one or more {@code files} parts.
     */
    @PostMapping(value = "/{post-id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add attachments to an existing post (author only)")
    public ResponseEntity<DtoHomePostResponse> addAttachments(
            @PathVariable("post-id") Long postId,
            @RequestPart("files") List<MultipartFile> files,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.addAttachments(postId, files, connectedUser));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Delete an entire post (including all its attachments). Only the author can do this.
     */
    @DeleteMapping("/{post-id}")
    @Operation(summary = "Delete a post (author only)")
    public ResponseEntity<Void> deletePost(
            @PathVariable("post-id") Long postId,
            Authentication connectedUser
    ) {
        serviceHomePost.deletePost(postId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a single attachment from a post. Only the author can do this.
     */
    @DeleteMapping("/{post-id}/attachments/{attachment-id}")
    @Operation(summary = "Delete a single attachment from a post (author only)")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable("post-id") Long postId,
            @PathVariable("attachment-id") Long attachmentId,
            Authentication connectedUser
    ) {
        serviceHomePost.deleteAttachment(postId, attachmentId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // LIKES
    // =========================================================================

    /**
     * Toggle like on a post. Calling it again on an already-liked post removes the like.
     */
    @PostMapping("/{post-id}/likes")
    @Operation(summary = "Toggle like on a post (like / unlike)")
    public ResponseEntity<DtoPostLikeResponse> toggleLike(
            @PathVariable("post-id") Long postId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.toggleLike(postId, connectedUser));
    }

    /**
     * Returns current like count and whether the authenticated user liked the post.
     */
    @GetMapping("/{post-id}/likes")
    @Operation(summary = "Get like status for a post")
    public ResponseEntity<DtoPostLikeResponse> getLikeStatus(
            @PathVariable("post-id") Long postId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.getLikeStatus(postId, connectedUser));
    }

    // =========================================================================
    // COMMENTS
    // =========================================================================

    /**
     * Add a comment to a post
     */
    @PostMapping("/{post-id}/comments")
    @Operation(summary = "Add a comment to a post")
    public ResponseEntity<DtoPostCommentResponse> addComment(
            @PathVariable("post-id") Long postId,
            @Valid @RequestBody DtoPostCommentRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.addComment(postId, request, connectedUser));
    }

    /**
     * Get paged comments for a post (oldest first)
     */
    @GetMapping("/{post-id}/comments")
    @Operation(summary = "Get paged comments for a post (oldest first)")
    public ResponseEntity<PageResponse<DtoPostCommentResponse>> getComments(
            @PathVariable("post-id") Long postId,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "20", required = false) int size
    ) {
        return ResponseEntity.ok(serviceHomePost.getComments(postId, page, size));
    }

    /**
     * Edit a comment (comment author only)
     */
    @PutMapping("/{post-id}/comments/{comment-id}")
    @Operation(summary = "Edit a comment (comment author only)")
    public ResponseEntity<DtoPostCommentResponse> updateComment(
            @PathVariable("post-id") Long postId,
            @PathVariable("comment-id") Long commentId,
            @Valid @RequestBody DtoPostCommentRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.updateComment(postId, commentId, request, connectedUser));
    }

    /**
     * Delete a comment (comment author or post author)
     */
    @DeleteMapping("/{post-id}/comments/{comment-id}")
    @Operation(summary = "Delete a comment (comment author or post author)")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("post-id") Long postId,
            @PathVariable("comment-id") Long commentId,
            Authentication connectedUser
    ) {
        serviceHomePost.deleteComment(postId, commentId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // SHARES
    // =========================================================================

    /**
     * Records a share event and returns the total share count + a shareable URL.
     */
    @PostMapping("/{post-id}/shares")
    @Operation(summary = "Record a share and get the shareable URL")
    public ResponseEntity<DtoPostShareResponse> sharePost(
            @PathVariable("post-id") Long postId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceHomePost.sharePost(postId, connectedUser));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns true for MIME types that should be rendered inline by the browser.
     */
    private boolean isInlineType(MediaType mediaType) {
        return mediaType.isCompatibleWith(MediaType.IMAGE_JPEG)
                || mediaType.isCompatibleWith(MediaType.IMAGE_PNG)
                || mediaType.isCompatibleWith(MediaType.IMAGE_GIF)
                || mediaType.isCompatibleWith(MediaType.parseMediaType("image/webp"))
                || mediaType.isCompatibleWith(MediaType.parseMediaType("application/pdf"));
    }
}


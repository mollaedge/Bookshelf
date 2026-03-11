package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostRequest;
import com.arturmolla.bookshelf.model.dto.DtoHomePostResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostUpdateRequest;
import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import com.arturmolla.bookshelf.model.entity.EntityPostAttachment;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryHomePost;
import com.arturmolla.bookshelf.service.mapper.MapperHomePost;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ServiceHomePost {

    private static final String POST_NOT_FOUND = "Post not found with id: ";

    /**
     * Files larger than this threshold will be compressed (images only).
     */
    private static final long COMPRESS_THRESHOLD_BYTES = 2 * 1024 * 1024; // 2 MB

    private final RepositoryHomePost repositoryHomePost;
    private final MapperHomePost mapperHomePost;
    private final ServiceFileStorage serviceFileStorage;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Creates a new post (text + optional attachments) for the authenticated user.
     *
     * @param request       post title and content
     * @param files         optional list of attachments (images / PDFs)
     * @param connectedUser the authenticated user
     * @return id of the newly created post
     */
    public Long createPost(DtoHomePostRequest request,
                           List<MultipartFile> files,
                           Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();

        EntityHomePost post = EntityHomePost.builder()
                .title(request.title())
                .content(request.content())
                .author(user)
                .build();

        if (files != null && !files.isEmpty()) {
            List<EntityPostAttachment> attachments = buildAttachments(files, post);
            post.getAttachments().addAll(attachments);
        }

        EntityHomePost saved = repositoryHomePost.save(post);
        log.info("Post created with id={} by userId={}", saved.getId(), user.getId());
        return saved.getId();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /**
     * Returns a single post by id.
     */
    public DtoHomePostResponse getPostById(Long postId) {
        EntityHomePost post = findPostOrThrow(postId);
        return mapperHomePost.toResponse(post);
    }

    /**
     * Returns all posts ordered by creation date descending (newest first), paged.
     */
    public PageResponse<DtoHomePostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EntityHomePost> pageResult = repositoryHomePost.findAllByOrderByCreatedDateDesc(pageable);
        return toPageResponse(pageResult);
    }

    /**
     * Returns posts created by the currently authenticated user, newest first.
     */
    public PageResponse<DtoHomePostResponse> getMyPosts(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<EntityHomePost> pageResult =
                repositoryHomePost.findByAuthorIdOrderByCreatedDateDesc(user.getId(), pageable);
        return toPageResponse(pageResult);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    /**
     * Updates title / content of an existing post. Only the post author can do this.
     * Attachments are managed separately via {@link #addAttachments} / {@link #deleteAttachment}.
     */
    public DtoHomePostResponse updatePost(Long postId,
                                          DtoHomePostUpdateRequest request,
                                          Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);
        assertOwnership(post, user);

        if (request.title() != null) post.setTitle(request.title());
        if (request.content() != null) post.setContent(request.content());

        EntityHomePost saved = repositoryHomePost.save(post);
        log.info("Post id={} updated by userId={}", postId, user.getId());
        return mapperHomePost.toResponse(saved);
    }

    /**
     * Adds new attachments to an existing post. Only the post author can do this.
     */
    public DtoHomePostResponse addAttachments(Long postId,
                                              List<MultipartFile> files,
                                              Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);
        assertOwnership(post, user);

        if (files != null && !files.isEmpty()) {
            List<EntityPostAttachment> attachments = buildAttachments(files, post);
            post.getAttachments().addAll(attachments);
        }

        EntityHomePost saved = repositoryHomePost.save(post);
        log.info("Added {} attachment(s) to postId={} by userId={}", files == null ? 0 : files.size(), postId, user.getId());
        return mapperHomePost.toResponse(saved);
    }

    /**
     * Removes a single attachment from a post. Only the post author can do this.
     */
    public void deleteAttachment(Long postId, Long attachmentId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);
        assertOwnership(post, user);

        boolean removed = post.getAttachments().removeIf(a -> Objects.equals(a.getId(), attachmentId));
        if (!removed) {
            throw new EntityNotFoundException("Attachment not found with id: " + attachmentId);
        }
        repositoryHomePost.save(post);
        log.info("Attachment id={} removed from postId={} by userId={}", attachmentId, postId, user.getId());
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Deletes a post entirely. Only the post author can do this.
     */
    public void deletePost(Long postId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);
        assertOwnership(post, user);

        repositoryHomePost.delete(post);
        log.info("Post id={} deleted by userId={}", postId, user.getId());
    }

    /**
     * Retrieves the raw bytes of a specific attachment.
     */
    public EntityPostAttachment getAttachmentEntity(Long postId, Long attachmentId) {
        EntityHomePost post = findPostOrThrow(postId);
        return post.getAttachments().stream()
                .filter(a -> Objects.equals(a.getId(), attachmentId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found with id: " + attachmentId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private EntityHomePost findPostOrThrow(Long postId) {
        return repositoryHomePost.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND + postId));
    }

    private void assertOwnership(EntityHomePost post, User user) {
        if (!Objects.equals(post.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You are not the author of this post");
        }
    }

    private List<EntityPostAttachment> buildAttachments(List<MultipartFile> files, EntityHomePost post) {
        List<EntityPostAttachment> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                byte[] bytes = file.getBytes();
                String contentType = file.getContentType();

                // Compress large images (skip for PDFs and other non-image types)
                if (bytes.length > COMPRESS_THRESHOLD_BYTES
                        && contentType != null
                        && contentType.startsWith("image/")) {
                    log.info("Compressing attachment '{}' ({} bytes)", file.getOriginalFilename(), bytes.length);
                    bytes = serviceFileStorage.compressImageBytes(bytes);
                    contentType = "image/jpeg";
                    log.info("Compressed to {} bytes", bytes.length);
                }

                result.add(EntityPostAttachment.builder()
                        .post(post)
                        .data(bytes)
                        .contentType(contentType)
                        .fileName(file.getOriginalFilename())
                        .fileSize((long) bytes.length)
                        .uploadedAt(LocalDateTime.now())
                        .build());
            } catch (IOException e) {
                log.error("Failed to read attachment '{}': {}", file.getOriginalFilename(), e.getMessage());
                throw new IllegalArgumentException("Could not process file: " + file.getOriginalFilename(), e);
            }
        }
        return result;
    }

    private PageResponse<DtoHomePostResponse> toPageResponse(Page<EntityHomePost> page) {
        List<DtoHomePostResponse> content = page.getContent()
                .stream()
                .map(mapperHomePost::toResponse)
                .toList();
        return PageResponse.<DtoHomePostResponse>builder()
                .content(content)
                .number(page.getNumber())
                .size(page.getSize())
                .totalElement(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}


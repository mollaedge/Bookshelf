package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostRequest;
import com.arturmolla.bookshelf.model.dto.DtoHomePostResponse;
import com.arturmolla.bookshelf.model.dto.DtoHomePostUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoPostCommentRequest;
import com.arturmolla.bookshelf.model.dto.DtoPostCommentResponse;
import com.arturmolla.bookshelf.model.dto.DtoPostLikeResponse;
import com.arturmolla.bookshelf.model.dto.DtoPostShareResponse;
import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import com.arturmolla.bookshelf.model.entity.EntityPostAttachment;
import com.arturmolla.bookshelf.model.entity.EntityPostComment;
import com.arturmolla.bookshelf.model.entity.EntityPostLike;
import com.arturmolla.bookshelf.model.entity.EntityPostShare;
import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryHomePost;
import com.arturmolla.bookshelf.repository.RepositoryPostComment;
import com.arturmolla.bookshelf.repository.RepositoryPostLike;
import com.arturmolla.bookshelf.repository.RepositoryPostShare;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.service.mapper.MapperHomePost;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final RepositoryPostLike repositoryPostLike;
    private final RepositoryPostComment repositoryPostComment;
    private final RepositoryPostShare repositoryPostShare;
    private final RepositoryUser repositoryUser;
    private final MapperHomePost mapperHomePost;
    private final ServiceFileStorage serviceFileStorage;
    private final ServiceNotification serviceNotification;

    @Value("${application.frontend.url:http://localhost:4200}")
    private String frontendUrl;

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
    public DtoHomePostResponse getPostById(Long postId, Authentication connectedUser) {
        EntityHomePost post = findPostOrThrow(postId);
        Long currentUserId = connectedUser != null ? ((User) connectedUser.getPrincipal()).getId() : null;
        return toResponseWithCounts(post, currentUserId);
    }

    /**
     * Returns all posts ordered by creation date descending (newest first), paged.
     */
    public PageResponse<DtoHomePostResponse> getAllPosts(int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EntityHomePost> pageResult = repositoryHomePost.findAllByOrderByCreatedDateDesc(pageable);
        Long currentUserId = connectedUser != null ? ((User) connectedUser.getPrincipal()).getId() : null;
        return toPageResponse(pageResult, currentUserId);
    }

    /**
     * Returns posts created by the currently authenticated user, newest first.
     */
    public PageResponse<DtoHomePostResponse> getMyPosts(int page, int size, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<EntityHomePost> pageResult =
                repositoryHomePost.findByAuthorIdOrderByCreatedDateDesc(user.getId(), pageable);
        return toPageResponse(pageResult, user.getId());
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
        return toResponseWithCounts(saved, user.getId());
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
        return toResponseWithCounts(saved, user.getId());
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
    // LIKES
    // -------------------------------------------------------------------------

    /**
     * Toggles the like on a post for the current user.
     * If already liked → unliked; if not liked → liked.
     */
    public DtoPostLikeResponse toggleLike(Long postId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);

        repositoryPostLike.findByPostIdAndUserId(postId, user.getId()).ifPresentOrElse(
                like -> {
                    repositoryPostLike.delete(like);
                    log.info("Post id={} unliked by userId={}", postId, user.getId());
                },
                () -> {
                    EntityPostLike like = EntityPostLike.builder()
                            .post(post)
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .build();
                    repositoryPostLike.save(like);
                    log.info("Post id={} liked by userId={}", postId, user.getId());
                    // Notify post author
                    serviceNotification.notify(
                            post.getAuthor(), user,
                            NotificationType.POST_LIKED,
                            user.getFullName() + " liked your post",
                            "\"" + (post.getTitle() != null ? post.getTitle() : "your post") + "\" received a like.",
                            postId, "POST"
                    );
                }
        );

        long likeCount = repositoryPostLike.countByPostId(postId);
        boolean likedByCurrentUser = repositoryPostLike.existsByPostIdAndUserId(postId, user.getId());
        return new DtoPostLikeResponse(likeCount, likedByCurrentUser);
    }

    /**
     * Returns the like status for the current user on a specific post.
     */
    public DtoPostLikeResponse getLikeStatus(Long postId, Authentication connectedUser) {
        findPostOrThrow(postId);
        var user = (User) connectedUser.getPrincipal();
        long likeCount = repositoryPostLike.countByPostId(postId);
        boolean likedByCurrentUser = repositoryPostLike.existsByPostIdAndUserId(postId, user.getId());
        return new DtoPostLikeResponse(likeCount, likedByCurrentUser);
    }

    // -------------------------------------------------------------------------
    // COMMENTS
    // -------------------------------------------------------------------------

    /**
     * Adds a comment to a post.
     */
    public DtoPostCommentResponse addComment(Long postId,
                                             DtoPostCommentRequest request,
                                             Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);

        EntityPostComment comment = EntityPostComment.builder()
                .post(post)
                .author(user)
                .content(request.content())
                .build();

        EntityPostComment saved = repositoryPostComment.save(comment);
        log.info("Comment id={} added to postId={} by userId={}", saved.getId(), postId, user.getId());

        // Notify post author
        serviceNotification.notify(
                post.getAuthor(), user,
                NotificationType.POST_COMMENTED,
                user.getFullName() + " commented on your post",
                user.getFullName() + ": \"" + truncate(request.content(), 100) + "\"",
                postId, "POST"
        );

        // Notify tagged users
        if (request.taggedUserIds() != null) {
            for (Long taggedId : request.taggedUserIds()) {
                repositoryUser.findById(taggedId).ifPresent(tagged ->
                        serviceNotification.notify(
                                tagged, user,
                                NotificationType.TAGGED,
                                user.getFullName() + " mentioned you in a comment",
                                "\"" + truncate(request.content(), 100) + "\"",
                                saved.getId(), "COMMENT"
                        )
                );
            }
        }

        return mapperHomePost.toCommentResponse(saved);
    }

    /**
     * Edits a comment. Only the comment author can do this.
     */
    public DtoPostCommentResponse updateComment(Long postId,
                                                Long commentId,
                                                DtoPostCommentRequest request,
                                                Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityPostComment comment = findCommentOrThrow(commentId);
        assertCommentBelongsToPost(comment, postId);
        assertCommentOwnership(comment, user);

        comment.setContent(request.content());
        EntityPostComment saved = repositoryPostComment.save(comment);
        log.info("Comment id={} updated by userId={}", commentId, user.getId());
        return mapperHomePost.toCommentResponse(saved);
    }

    /**
     * Deletes a comment. The comment author or the post author can do this.
     */
    public void deleteComment(Long postId, Long commentId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityPostComment comment = findCommentOrThrow(commentId);
        assertCommentBelongsToPost(comment, postId);

        EntityHomePost post = findPostOrThrow(postId);
        boolean isCommentAuthor = Objects.equals(comment.getCreatedBy(), user.getId());
        boolean isPostAuthor = Objects.equals(post.getCreatedBy(), user.getId());

        if (!isCommentAuthor && !isPostAuthor) {
            throw new OperationNotPermittedException("You are not allowed to delete this comment");
        }

        repositoryPostComment.delete(comment);
        log.info("Comment id={} deleted by userId={}", commentId, user.getId());
    }

    /**
     * Returns all comments for a post, ordered oldest-first, paged.
     */
    public PageResponse<DtoPostCommentResponse> getComments(Long postId, int page, int size) {
        findPostOrThrow(postId);
        Pageable pageable = PageRequest.of(page, size);
        Page<EntityPostComment> pageResult =
                repositoryPostComment.findByPostIdOrderByCreatedDateAsc(postId, pageable);

        List<DtoPostCommentResponse> content = pageResult.getContent()
                .stream()
                .map(mapperHomePost::toCommentResponse)
                .toList();

        return PageResponse.<DtoPostCommentResponse>builder()
                .content(content)
                .number(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElement(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    // -------------------------------------------------------------------------
    // SHARES
    // -------------------------------------------------------------------------

    /**
     * Records a share event and returns the share count + a shareable URL.
     */
    public DtoPostShareResponse sharePost(Long postId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityHomePost post = findPostOrThrow(postId);

        EntityPostShare share = EntityPostShare.builder()
                .post(post)
                .user(user)
                .sharedAt(LocalDateTime.now())
                .build();
        repositoryPostShare.save(share);
        log.info("Post id={} shared by userId={}", postId, user.getId());

        // Notify post author
        serviceNotification.notify(
                post.getAuthor(), user,
                NotificationType.POST_SHARED,
                user.getFullName() + " shared your post",
                "\"" + (post.getTitle() != null ? post.getTitle() : "your post") + "\" was shared.",
                postId, "POST"
        );

        long shareCount = repositoryPostShare.countByPostId(postId);
        String shareUrl = frontendUrl + "/posts/" + postId;
        return new DtoPostShareResponse(shareCount, shareUrl);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private EntityHomePost findPostOrThrow(Long postId) {
        return repositoryHomePost.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND + postId));
    }

    private EntityPostComment findCommentOrThrow(Long commentId) {
        return repositoryPostComment.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + commentId));
    }

    private void assertOwnership(EntityHomePost post, User user) {
        if (!Objects.equals(post.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You are not the author of this post");
        }
    }

    private void assertCommentOwnership(EntityPostComment comment, User user) {
        if (!Objects.equals(comment.getCreatedBy(), user.getId())) {
            throw new OperationNotPermittedException("You are not the author of this comment");
        }
    }

    private void assertCommentBelongsToPost(EntityPostComment comment, Long postId) {
        if (!Objects.equals(comment.getPost().getId(), postId)) {
            throw new OperationNotPermittedException("Comment does not belong to post id: " + postId);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    private DtoHomePostResponse toResponseWithCounts(EntityHomePost post, Long currentUserId) {        long likeCount = repositoryPostLike.countByPostId(post.getId());
        long commentCount = repositoryPostComment.countByPostId(post.getId());
        long shareCount = repositoryPostShare.countByPostId(post.getId());
        boolean likedByCurrentUser = currentUserId != null
                && repositoryPostLike.existsByPostIdAndUserId(post.getId(), currentUserId);
        return mapperHomePost.toResponse(post, likeCount, commentCount, shareCount, likedByCurrentUser);
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

    private PageResponse<DtoHomePostResponse> toPageResponse(Page<EntityHomePost> page, Long currentUserId) {
        List<DtoHomePostResponse> content = page.getContent()
                .stream()
                .map(post -> toResponseWithCounts(post, currentUserId))
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


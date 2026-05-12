package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminFeedbackUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminSystemCommentRequest;
import com.arturmolla.bookshelf.model.dto.DtoAppFeedback;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedback;
import com.arturmolla.bookshelf.model.entity.EntityAppFeedbackComment;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryAppFeedback;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.service.mapper.MapperAppFeedback;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceAdminAppFeedback {

    private static final String FEEDBACK_NOT_FOUND = "App feedback not found with id: ";
    private static final String SYSTEM_AUTHOR_NAME = "System"; // used for admin-posted system comments

    private final RepositoryAppFeedback repositoryAppFeedback;
    private final RepositoryUser repositoryUser;
    private final MapperAppFeedback mapperAppFeedback;
    private final ServiceNotification serviceNotification;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    public PageResponse<DtoAppFeedback> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityAppFeedback> feedbacks = repositoryAppFeedback.findAll(pageable);
        List<DtoAppFeedback> content = feedbacks.stream()
                .map(f -> mapperAppFeedback.toDto(f, null))
                .toList();
        return toPageResponse(content, feedbacks);
    }

    public PageResponse<DtoAppFeedback> getAllByStatus(AppFeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<EntityAppFeedback> feedbacks = repositoryAppFeedback.findAllByStatus(status, pageable);
        List<DtoAppFeedback> content = feedbacks.stream()
                .map(f -> mapperAppFeedback.toDto(f, null))
                .toList();
        return toPageResponse(content, feedbacks);
    }

    public DtoAppFeedback getById(Long id) {
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        return mapperAppFeedback.toDto(feedback, null);
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    public DtoAppFeedback create(AppFeedbackRequest request, Authentication connectedUser) {
        var admin = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = mapperAppFeedback.toEntity(request);
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), admin.getId());
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    public DtoAppFeedback update(Long id, DtoAdminFeedbackUpdateRequest request, Authentication connectedUser) {
        var admin = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));

        if (request.getTitle() != null) {
            feedback.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            feedback.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && request.getStatus() != feedback.getStatus()) {
            AppFeedbackStatus previousStatus = feedback.getStatus();
            feedback.setStatus(request.getStatus());

            // Notify the feedback author about the status change
            repositoryUser.findById(feedback.getCreatedBy()).ifPresent(author ->
                    serviceNotification.notify(
                            author,
                            admin,
                            NotificationType.FEEDBACK_STATUS_CHANGED,
                            "Your feedback status changed",
                            "\"" + feedback.getTitle() + "\" changed from "
                                    + previousStatus + " to " + request.getStatus() + ".",
                            id,
                            "FEEDBACK"
                    )
            );
        }

        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), admin.getId());
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    public void delete(Long id) {
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));
        repositoryAppFeedback.delete(feedback);
    }

    // -----------------------------------------------------------------------
    // Comments (system)
    // -----------------------------------------------------------------------

    /**
     * Adds a system comment to a feedback on behalf of the admin.
     * The comment is stored with {@code authorId = null} and
     * {@code authorName = "System"}, making it visually distinct from
     * regular user comments. The feedback author also receives a notification.
     */
    public DtoAppFeedback addSystemComment(Long id, DtoAdminSystemCommentRequest request,
                                           Authentication connectedUser) {
        var admin = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + id));

        EntityAppFeedbackComment comment = EntityAppFeedbackComment.builder()
                .authorId(null)          // null signals a system comment
                .authorName(SYSTEM_AUTHOR_NAME)
                .message(request.message())
                .createdAt(LocalDateTime.now())
                .build();

        feedback.getComments().add(comment);

        // Notify the feedback author
        repositoryUser.findById(feedback.getCreatedBy()).ifPresent(author ->
                serviceNotification.notify(
                        author,
                        admin,
                        NotificationType.FEEDBACK_COMMENTED,
                        "New update on your feedback",
                        "\"" + feedback.getTitle() + "\": " + request.message(),
                        id,
                        "FEEDBACK"
                )
        );

        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), admin.getId());
    }

    /**
     * Deletes the comment at the given zero-based {@code index} from the feedback's
     * comment list. Only system comments (authorId == null) can be deleted via this
     * endpoint; passing an index that points to a regular user comment throws an exception.
     */
    public DtoAppFeedback deleteComment(Long feedbackId, int index, Authentication connectedUser) {
        var admin = (User) connectedUser.getPrincipal();
        EntityAppFeedback feedback = repositoryAppFeedback.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + feedbackId));

        List<EntityAppFeedbackComment> comments = feedback.getComments();
        if (index < 0 || index >= comments.size()) {
            throw new IllegalArgumentException("Comment index out of bounds: " + index);
        }

        comments.remove(index);
        return mapperAppFeedback.toDto(repositoryAppFeedback.save(feedback), admin.getId());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static PageResponse<DtoAppFeedback> toPageResponse(List<DtoAppFeedback> content,
                                                                Page<EntityAppFeedback> page) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}


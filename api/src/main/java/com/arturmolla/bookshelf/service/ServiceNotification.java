package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoNotificationResponse;
import com.arturmolla.bookshelf.model.entity.EntityNotification;
import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryNotification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ServiceNotification {

    private final RepositoryNotification repositoryNotification;

    // ─────────────────────────────────────────────────────────────
    //  Internal helper – called by other services
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a notification. Silently skips if the actor and recipient are the same
     * (no one wants a notification for their own action).
     *
     * @param recipient     user who will receive the notification
     * @param actor         user who triggered the event (null = system notification)
     * @param type          notification category
     * @param title         short heading shown in the notification bell
     * @param message       full description
     * @param referenceId   ID of the related entity (post / book / comment / feedback)
     * @param referenceType label for the related entity ("POST", "BOOK", "COMMENT", "FEEDBACK")
     */
    public void notify(User recipient,
                       User actor,
                       NotificationType type,
                       String title,
                       String message,
                       Long referenceId,
                       String referenceType) {
        // Don't notify users about their own actions
        if (actor != null && Objects.equals(recipient.getId(), actor.getId())) {
            return;
        }
        EntityNotification notification = EntityNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        repositoryNotification.save(notification);
        log.debug("Notification [{}] created for userId={}", type, recipient.getId());
    }

    // ─────────────────────────────────────────────────────────────
    //  Public API – consumed by the controller
    // ─────────────────────────────────────────────────────────────

    /** Returns the authenticated user's notifications, newest first, paged. */
    public PageResponse<DtoNotificationResponse> getMyNotifications(int page, int size,
                                                                     Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        Page<EntityNotification> result = repositoryNotification
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));

        List<DtoNotificationResponse> content = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DtoNotificationResponse>builder()
                .content(content)
                .number(result.getNumber())
                .size(result.getSize())
                .totalElement(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    /** Returns the number of unread notifications for the authenticated user. */
    public long getUnreadCount(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        return repositoryNotification.countByRecipientIdAndReadFalse(user.getId());
    }

    /**
     * Marks a single notification as read.
     *
     * @throws EntityNotFoundException     if the notification does not exist
     * @throws IllegalArgumentException    if the notification does not belong to the user
     */
    public DtoNotificationResponse markAsRead(Long notificationId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        EntityNotification notification = repositoryNotification.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        if (!Objects.equals(notification.getRecipient().getId(), user.getId())) {
            throw new IllegalArgumentException("This notification does not belong to you");
        }

        notification.setRead(true);
        return toResponse(repositoryNotification.save(notification));
    }

    /** Marks ALL notifications of the authenticated user as read. */
    public void markAllAsRead(Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        repositoryNotification.markAllReadByRecipientId(user.getId());
        log.debug("All notifications marked as read for userId={}", user.getId());
    }

    /**
     * Deletes a single notification. Only the recipient may delete it.
     *
     * @throws EntityNotFoundException  if the notification does not exist or does not belong to the user
     */
    public void deleteNotification(Long notificationId, Authentication connectedUser) {
        var user = (User) connectedUser.getPrincipal();
        // Uses the combined key so it's a no-op if the record doesn't exist or belongs to someone else
        repositoryNotification.deleteByIdAndRecipientId(notificationId, user.getId());
        log.debug("Notification id={} deleted by userId={}", notificationId, user.getId());
    }

    // ─────────────────────────────────────────────────────────────
    //  Private mapper
    // ─────────────────────────────────────────────────────────────

    private DtoNotificationResponse toResponse(EntityNotification n) {
        return DtoNotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .actorId(n.getActor() != null ? n.getActor().getId() : null)
                .actorName(n.getActor() != null ? n.getActor().getFullName() : null)
                .createdAt(n.getCreatedAt())
                .build();
    }
}


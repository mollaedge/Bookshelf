package com.arturmolla.bookshelf.model.entity;

import com.arturmolla.bookshelf.model.enums.NotificationType;
import com.arturmolla.bookshelf.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipient_id, created_at DESC"),
                @Index(name = "idx_notification_unread", columnList = "recipient_id, is_read")
        })
public class EntityNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who receives this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** The user whose action triggered this notification (null for system-generated). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private NotificationType type;

    /** Short heading shown in the notification bell. */
    @Column(nullable = false)
    private String title;

    /** Full human-readable message. */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Whether the recipient has read this notification. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** ID of the entity this notification refers to (post id, book id, etc.). */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Type of the referenced entity: POST, BOOK, COMMENT, FEEDBACK. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}


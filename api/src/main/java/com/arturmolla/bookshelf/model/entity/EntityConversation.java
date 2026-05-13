package com.arturmolla.bookshelf.model.entity;

import com.arturmolla.bookshelf.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a direct-message conversation between exactly two users.
 * <p>
 * To prevent duplicate rows the smaller userId is always stored in
 * {@code user1} and the larger in {@code user2}. The database enforces
 * this via a {@code CHECK} constraint and a {@code UNIQUE} constraint.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "conversation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversation",
                columnNames = {"user1_id", "user2_id"}
        ),
        indexes = {
                @Index(name = "idx_conversation_user1", columnList = "user1_id"),
                @Index(name = "idx_conversation_user2", columnList = "user2_id")
        }
)
public class EntityConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User with the smaller ID (canonical ordering). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /** User with the larger ID (canonical ordering). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Updated every time a new message is persisted in this conversation. */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;
}


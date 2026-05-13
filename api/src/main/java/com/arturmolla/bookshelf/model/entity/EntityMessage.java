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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single message sent within a {@link EntityConversation}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "message",
        indexes = {
                @Index(name = "idx_message_conversation", columnList = "conversation_id, created_at"),
                @Index(name = "idx_message_unread",       columnList = "conversation_id, is_read")
        }
)
public class EntityMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private EntityConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * The message this message is replying to, or {@code null} for a top-level message.
     * The FK is SET NULL on delete so that if the original is removed the reply still exists.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private EntityMessage replyTo;

    /**
     * Optional: Media data (image/file) attached to this message. Null if no media.
     */
    @Column(name = "media_data", columnDefinition = "BYTEA")
    private byte[] mediaData;

    /**
     * MIME type of the media (e.g., image/jpeg, application/pdf). Null if no media.
     */
    @Column(name = "media_type")
    private String mediaType;

    /**
     * Original filename of the media. Null if no media.
     */
    @Column(name = "media_name")
    private String mediaName;

    /**
     * Size of the media in bytes. Null if no media.
     */
    @Column(name = "media_size")
    private Long mediaSize;

    /** True once the recipient has acknowledged the message. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}


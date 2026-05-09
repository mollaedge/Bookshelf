package com.arturmolla.bookshelf.model.entity;

import com.arturmolla.bookshelf.model.enums.RelationStatus;
import com.arturmolla.bookshelf.model.enums.RelationType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "user_relation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_relation",
                columnNames = {"requester_id", "addressee_id", "relation_type"}
        ),
        indexes = {
                @Index(name = "idx_relation_addressee", columnList = "addressee_id, relation_type, status"),
                @Index(name = "idx_relation_requester", columnList = "requester_id, relation_type, status")
        }
)
public class EntityUserRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who initiated the relation (sender of request / follower). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** The user who is the target of the relation (receiver / followed user). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    /** Whether this record represents a friend request or a follow. */
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private RelationType relationType;

    /**
     * Current status of the relation.
     * For {@link RelationType#FRIEND_REQUEST}: PENDING → ACCEPTED or REJECTED.
     * For {@link RelationType#FOLLOW}: always ACCEPTED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RelationStatus status = RelationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}


package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryConversation extends JpaRepository<EntityConversation, Long> {

    /**
     * Finds the conversation between two users regardless of which one was
     * stored as user1 / user2 (canonical ordering normalises IDs on write,
     * but this query is defensive).
     */
    @Query("""
            SELECT c FROM EntityConversation c
            WHERE (c.user1.id = :a AND c.user2.id = :b)
               OR (c.user1.id = :b AND c.user2.id = :a)
            """)
    Optional<EntityConversation> findByUsers(@Param("a") Long userAId, @Param("b") Long userBId);

    /**
     * Returns all conversations in which the given user participates,
     * ordered by most-recent activity.
     */
    @Query("""
            SELECT c FROM EntityConversation c
            WHERE c.user1.id = :userId OR c.user2.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    Page<EntityConversation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Returns all conversations involving a user as a flat list (used for bulk deletion). */
    @Query("SELECT c FROM EntityConversation c WHERE c.user1.id = :userId OR c.user2.id = :userId")
    List<EntityConversation> findAllByUserId(@Param("userId") Long userId);
}


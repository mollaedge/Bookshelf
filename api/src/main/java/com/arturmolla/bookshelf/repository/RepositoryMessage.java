package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryMessage extends JpaRepository<EntityMessage, Long> {

    /** Returns messages for a conversation, oldest first (chat order). */
    Page<EntityMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    /** Count of unread messages sent TO the given user in a specific conversation. */
    @Query("""
            SELECT COUNT(m) FROM EntityMessage m
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.read = false
            """)
    long countUnreadForUser(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId);

    /** Marks every unread message sent by the OTHER user in a conversation as read. */
    @Modifying
    @Query("""
            UPDATE EntityMessage m
               SET m.read = true
             WHERE m.conversation.id = :conversationId
               AND m.sender.id <> :userId
               AND m.read = false
            """)
    void markAllReadInConversation(@Param("conversationId") Long conversationId,
                                   @Param("userId") Long userId);

    /** Last message in a conversation — used to populate conversation list previews. */
    @Query("""
            SELECT m FROM EntityMessage m
            WHERE m.conversation.id = :conversationId
            ORDER BY m.createdAt DESC
            LIMIT 1
            """)
    java.util.Optional<EntityMessage> findLastMessage(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("DELETE FROM EntityMessage m WHERE m.conversation.id IN :conversationIds")
    void deleteAllByConversationIdIn(@Param("conversationIds") List<Long> conversationIds);
}


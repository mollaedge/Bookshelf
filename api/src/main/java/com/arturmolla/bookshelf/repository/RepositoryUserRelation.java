package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityUserRelation;
import com.arturmolla.bookshelf.model.enums.RelationStatus;
import com.arturmolla.bookshelf.model.enums.RelationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUserRelation extends JpaRepository<EntityUserRelation, Long> {

    /** Find a specific relation between two users of a given type. */
    Optional<EntityUserRelation> findByRequesterIdAndAddresseeIdAndRelationType(
            Long requesterId, Long addresseeId, RelationType type);

    /** Check whether a relation already exists. */
    boolean existsByRequesterIdAndAddresseeIdAndRelationType(
            Long requesterId, Long addresseeId, RelationType type);

    // ── Friend requests ───────────────────────────────────────────────────────

    /** Incoming pending friend requests for a user (they are the addressee). */
    Page<EntityUserRelation> findByAddresseeIdAndRelationTypeAndStatus(
            Long addresseeId, RelationType type, RelationStatus status, Pageable pageable);

    /** Outgoing pending friend requests sent by a user (they are the requester). */
    Page<EntityUserRelation> findByRequesterIdAndRelationTypeAndStatus(
            Long requesterId, RelationType type, RelationStatus status, Pageable pageable);

    // ── Friends (accepted) ────────────────────────────────────────────────────

    /**
     * Accepted friendships where the user is either the requester or the addressee.
     */
    @Query("""
            SELECT r FROM EntityUserRelation r
            WHERE r.relationType = 'FRIEND_REQUEST'
              AND r.status = 'ACCEPTED'
              AND (r.requester.id = :userId OR r.addressee.id = :userId)
            """)
    Page<EntityUserRelation> findFriends(@Param("userId") Long userId, Pageable pageable);

    // ── Follows ───────────────────────────────────────────────────────────────

    /** Users that :userId is following. */
    Page<EntityUserRelation> findByRequesterIdAndRelationType(
            Long requesterId, RelationType type, Pageable pageable);

    /** Users that follow :userId (followers). */
    Page<EntityUserRelation> findByAddresseeIdAndRelationType(
            Long addresseeId, RelationType type, Pageable pageable);

    // ── Counts ────────────────────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(r) FROM EntityUserRelation r
            WHERE r.relationType = 'FRIEND_REQUEST'
              AND r.status = 'ACCEPTED'
              AND (r.requester.id = :userId OR r.addressee.id = :userId)
            """)
    long countFriends(@Param("userId") Long userId);

    long countByRequesterIdAndRelationType(Long requesterId, RelationType type);

    long countByAddresseeIdAndRelationType(Long addresseeId, RelationType type);

    @Modifying
    @Query("DELETE FROM EntityUserRelation r WHERE r.requester.id = :userId OR r.addressee.id = :userId")
    void deleteAllInvolvingUser(@Param("userId") Long userId);
}


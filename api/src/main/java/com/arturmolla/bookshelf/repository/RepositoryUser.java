package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUser extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * Full-text user search across first name, last name, combined full name and e-mail.
     * The authenticated user is excluded from the results, as are users who are already
     * accepted friends. The query string is optional — if null or blank all users are returned.
     * Matching is case-insensitive and supports partial (contains) queries.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :currentUserId
              AND (
                  :query IS NULL
               OR :query = ''
               OR LOWER(u.firstname)                          LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.lastname)                           LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(CONCAT(u.firstname, ' ', u.lastname)) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email)                              LIKE LOWER(CONCAT('%', :query, '%'))
              )
              AND NOT EXISTS (
                  SELECT r FROM EntityUserRelation r
                  WHERE r.relationType = com.arturmolla.bookshelf.model.enums.RelationType.FRIEND_REQUEST
                    AND r.status       = com.arturmolla.bookshelf.model.enums.RelationStatus.ACCEPTED
                    AND (
                        (r.requester.id = :currentUserId AND r.addressee.id = u.id)
                     OR (r.requester.id = u.id           AND r.addressee.id = :currentUserId)
                    )
              )
            ORDER BY u.firstname ASC, u.lastname ASC
            """)
    Page<User> searchUsers(@Param("query") String query,
                           @Param("currentUserId") Long currentUserId,
                           Pageable pageable);
}

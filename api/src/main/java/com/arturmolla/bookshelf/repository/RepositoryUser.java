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
     * The searching user is excluded from the results.
     * Matching is case-insensitive and supports partial (contains) queries.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :excludeId
              AND (
                  LOWER(u.firstname)                        LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.lastname)                         LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(CONCAT(u.firstname, ' ', u.lastname)) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email)                            LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY u.firstname ASC, u.lastname ASC
            """)
    Page<User> searchUsers(@Param("query") String query,
                           @Param("excludeId") Long excludeId,
                           Pageable pageable);
}

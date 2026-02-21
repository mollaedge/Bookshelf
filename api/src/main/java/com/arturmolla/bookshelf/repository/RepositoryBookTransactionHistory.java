package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityBookTransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryBookTransactionHistory extends JpaRepository<EntityBookTransactionHistory, Long> {

    @Query("""
            SELECT history
            FROM EntityBookTransactionHistory history
            WHERE history.user.id = :userId""")
    Page<EntityBookTransactionHistory> findAllBorrowedBooks(Pageable pageable, Long userId);

    @Query("""
            SELECT history
            FROM EntityBookTransactionHistory history
            WHERE history.book.owner.id = :userId""")
    Page<EntityBookTransactionHistory> findAllReturnedBooks(Pageable pageable, Long userId);

    @Query("""
            SELECT (COUNT(*) > 0) AS isBorrowed
            FROM EntityBookTransactionHistory history
            WHERE history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returnApproved = false""")
    boolean isAlreadyBorrowedByUser(Long bookId, Long userId);

    @Query("""
            SELECT history
            FROM EntityBookTransactionHistory history
            WHERE history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returned = false
            AND history.returnApproved = false""")
    Optional<EntityBookTransactionHistory> findByBookIdAndUserId(Long bookId, Long userId);

    @Query("""
            SELECT history
            FROM EntityBookTransactionHistory history
            WHERE history.book.owner.id = :userId
            AND history.book.id = :bookId
            AND history.returned = true
            AND history.returnApproved = false""")
    Optional<EntityBookTransactionHistory> findByBookIdAndOwnerId(Long bookId, Long userId);

    @Query("""
            SELECT COUNT(h)
            FROM EntityBookTransactionHistory h
            WHERE h.user.id = :userId
              AND h.returned = true
              AND h.book.read = true""")
    long countByReturnedTrueAndUserIdAndReadTrue(Long userId);

    @Query("""
            SELECT COUNT(h)
            FROM EntityBookTransactionHistory h
            WHERE h.user.id = :userId
              AND h.returned = false""")
    long countByReturnedFalseAndUserId(Long userId);

    @Query("""
            SELECT COUNT(h)
            FROM EntityBookTransactionHistory h
            WHERE h.book.owner.id = :ownerId
              AND h.returnApproved = false
              AND h.returned = true""")
    long countPendingReturnsByOwnerId(Long ownerId);

    @Query(value = """
            SELECT TO_CHAR(h.last_modified_date, 'YYYY-MM') AS month,
                   COUNT(h.id)                              AS books_read
            FROM   book_transaction_history h
            WHERE  h.user_id  = :userId
              AND  h.returned = true
              AND  h.last_modified_date >= :from
            GROUP BY TO_CHAR(h.last_modified_date, 'YYYY-MM')
            ORDER BY month ASC""",
            nativeQuery = true)
    List<Object[]> findMonthlyReadingActivity(Long userId, LocalDateTime from);
}



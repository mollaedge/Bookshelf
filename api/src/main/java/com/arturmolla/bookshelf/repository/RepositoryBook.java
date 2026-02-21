package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityBook;
import com.arturmolla.bookshelf.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryBook extends JpaRepository<EntityBook, Long>, JpaSpecificationExecutor<EntityBook> {

    @Query("""
            SELECT book
            FROM EntityBook book
            WHERE book.archived = false
            AND book.shareable = true
            AND book.owner.id = :userId""")
    Page<EntityBook> findAllUsersBooks(Pageable pageable, Long userId);

    @Query("""
            SELECT book
            FROM EntityBook book
            WHERE book.archived = false
            AND book.shareable = true
            AND book.owner.id != :userId""")
    Page<EntityBook> findAllBooks(Pageable pageable, Long userId);

    long countByOwner(User owner);

    long countByOwnerAndReadTrue(User owner);

    @Query("""
            SELECT COALESCE(b.genre, 'Other') AS genre,
                   COUNT(h) AS cnt
            FROM EntityBookTransactionHistory h
            JOIN h.book b
            WHERE h.user.id = :userId
              AND h.returned = true
            GROUP BY COALESCE(b.genre, 'Other')
            ORDER BY cnt DESC""")
    List<Object[]> findGenreDistributionByUser(Long userId);

    Page<EntityBook> findByOwnerOrderByCreatedDateDesc(User owner, Pageable pageable);
}
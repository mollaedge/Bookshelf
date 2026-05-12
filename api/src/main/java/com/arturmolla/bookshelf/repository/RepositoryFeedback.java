package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryFeedback extends JpaRepository<EntityFeedback, Long> {

    @Query("""
            SELECT feedback
            FROM EntityFeedback feedback
            WHERE feedback.book.id = :bookId""")
    Page<EntityFeedback> findAllByBookId(Long bookId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM EntityFeedback f WHERE f.book.id IN :bookIds")
    void deleteAllByBookIdIn(@Param("bookIds") List<Long> bookIds);
}

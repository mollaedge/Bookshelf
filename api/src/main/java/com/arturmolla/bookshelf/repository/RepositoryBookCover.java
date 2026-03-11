package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityBookCover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryBookCover extends JpaRepository<EntityBookCover, Long> {

    Optional<EntityBookCover> findByBookId(Long bookId);

    void deleteByBookId(Long bookId);
}


package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityBookPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryBookPdf extends JpaRepository<EntityBookPdf, Long> {

    Optional<EntityBookPdf> findByBookId(Long bookId);

    void deleteByBookId(Long bookId);

    boolean existsByBookId(Long bookId);
}


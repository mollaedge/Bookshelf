package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityHomePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryHomePost extends JpaRepository<EntityHomePost, Long> {

    /**
     * Returns all posts ordered by creation date (newest first), paged.
     */
    Page<EntityHomePost> findAllByOrderByCreatedDateDesc(Pageable pageable);

    /**
     * Returns all posts belonging to a specific author, newest first.
     */
    Page<EntityHomePost> findByAuthorIdOrderByCreatedDateDesc(Long authorId, Pageable pageable);

    /** Returns all posts belonging to a specific author as a flat list (used for bulk deletion). */
    List<EntityHomePost> findByAuthorId(Long authorId);
}


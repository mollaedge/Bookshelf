package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityPostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryPostComment extends JpaRepository<EntityPostComment, Long> {

    Page<EntityPostComment> findByPostIdOrderByCreatedDateAsc(Long postId, Pageable pageable);

    long countByPostId(Long postId);
}


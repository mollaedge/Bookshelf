package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryPostLike extends JpaRepository<EntityPostLike, Long> {

    Optional<EntityPostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);
}


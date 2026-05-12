package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryPostLike extends JpaRepository<EntityPostLike, Long> {

    Optional<EntityPostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM EntityPostLike l WHERE l.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EntityPostLike l WHERE l.post.id IN :postIds")
    void deleteAllByPostIdIn(@Param("postIds") List<Long> postIds);
}


package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.entity.EntityPostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryPostComment extends JpaRepository<EntityPostComment, Long> {

    Page<EntityPostComment> findByPostIdOrderByCreatedDateAsc(Long postId, Pageable pageable);

    long countByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM EntityPostComment c WHERE c.author.id = :authorId")
    void deleteAllByAuthorId(@Param("authorId") Long authorId);

    @Modifying
    @Query("DELETE FROM EntityPostComment c WHERE c.post.id IN :postIds")
    void deleteAllByPostIdIn(@Param("postIds") List<Long> postIds);
}

